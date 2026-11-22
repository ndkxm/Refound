# 1. Kiến trúc tổng thể

Một app Android, một server Spring Boot (một instance trên Render free), một Postgres + Storage của Supabase. Mọi logic nghiệp vụ, khóa bí mật và lời gọi AI nằm ở server. App chỉ biết `EXPO_PUBLIC_API_URL`.

## Sơ đồ thành phần

```mermaid
flowchart LR
  subgraph Phone["Máy Android (APK)"]
    App["App Expo<br/>TanStack Query · Zustand<br/>MapLibre · Camera"]
  end

  subgraph Render["Render free (Docker, 512 MB)"]
    direction TB
    API["REST /api/v1<br/>Spring MVC + Security (JWT)"]
    WS["WebSocket /ws<br/>STOMP, chỉ đẩy tín hiệu"]
    Jobs["Job nền trong tiến trình<br/>@Scheduled: embedding + đối chiếu,<br/>hết hạn tin"]
    Push["Gửi push sau commit"]
  end

  subgraph Supa["Supabase free"]
    PG[("Postgres<br/>schema refound<br/>pgvector · PostGIS")]
    ST[("Storage<br/>bucket private item-images")]
  end

  Jina["Jina API<br/>jina-clip-v2 (768 chiều)"]
  Groq["Groq API<br/>mô tả ảnh (tùy chọn)"]
  Expo["Expo Push API → FCM"]
  OSM["Tile OpenStreetMap"]
  FB["Firebase Auth<br/>Google hoặc SMS OTP"]
  GK["Khóa công khai của Google<br/>(JWKS, để kiểm tra ID token)"]
  GHA["GitHub Actions<br/>cron 1 lần/ngày"]

  App -- "HTTPS JSON / multipart" --> API
  App <-- "WSS (tín hiệu)" --> WS
  App -- "GET ảnh qua signed URL (hết hạn 1h)" --> ST
  App -- tile --> OSM
  App -- "đăng nhập, nhận ID token" --> FB
  API -- "tải và cache khóa" --> GK
  API --> PG
  API -- "upload, ký URL (service key)" --> ST
  Jobs --> PG
  Jobs --> Jina
  Jobs -. "chỉ khi chọn phương án B" .-> Groq
  API -- "embed câu truy vấn" --> Jina
  Push --> Expo --> Phone
  GHA -- "GET /api/v1/health?db=true" --> API
```

Ghi chú:

- Đăng nhập: app đăng nhập Firebase bằng Google hoặc OTP, gửi ID token cho server một lần; server kiểm tra chữ ký bằng khóa công khai của Google rồi phát hành JWT riêng. Sau đó app chỉ dùng JWT của server (ADR-05).
- App **không** giữ khóa Supabase. Ảnh tải bằng signed URL do server cấp; URL không chứa khóa và hết hạn sau 1 giờ (ADR-06).
- WebSocket chỉ báo "có gì đó thay đổi"; dữ liệu thật luôn lấy lại qua REST. Mất WS thì app vẫn đúng, chỉ chậm hơn (ADR-07).
- Job nền chạy trong chính tiến trình Spring, trạng thái job lưu trong cột của bảng `items`, nên server ngủ hay khởi động lại không mất việc (ADR-01).

## Các lớp trong server

| Lớp              | Nội dung                                                                   | Ví dụ                                             |
| ---------------- | -------------------------------------------------------------------------- | ------------------------------------------------- |
| `presentation`   | Controller REST, DTO theo người xem, STOMP config                          | `ItemController`, `ItemPublicDto`                 |
| `application`    | Service, transaction, gọi policy và state machine, phát sự kiện            | `ClaimService.accept()`                           |
| `domain`         | Hàm thuần, không Spring, không DB: chấm điểm, máy trạng thái, policy quyền | `MatchScorer`, `ClaimStateMachine`, `ClaimAccess` |
| `infrastructure` | JDBC/JPA repository, client Jina/Groq/Storage/Expo Push                    | `JinaEmbeddingClient`                             |

Phần khó (đối chiếu, máy trạng thái, phân quyền) nằm ở `domain` dưới dạng hàm thuần, test bằng JUnit không cần DB. Đây là phần mỗi thành viên phải giải thích được khi bảo vệ.

## Luồng 1: đăng tin (nhặt được)

```mermaid
sequenceDiagram
  autonumber
  actor F as Finder
  participant A as App
  participant S as Server
  participant DB as Postgres
  participant ST as Storage
  F->>A: Chụp 1–4 ảnh, chọn danh mục, mô tả, vị trí, câu hỏi kín
  A->>A: Resize: ảnh lớn 1024px + ảnh nhỏ 512px (JPEG)
  A->>S: POST /items (multipart, JWT)
  S->>S: Validate (zod phía app, Bean Validation phía server)<br/>BR-09 đếm tin hôm nay, FR-MATCH-04 kiểm tra trùng
  alt nghi trùng và chưa có force=true
    S-->>A: 409 DUPLICATE_SUSPECTED (id tin cũ)
  end
  S->>ST: Upload ảnh (service key)
  S->>DB: TX: insert items(status=open, embedding_status=pending),<br/>item_images, verification_secrets
  S-->>A: 201 ItemOwnerDto
  S-)S: Sau commit: đánh thức job embedding
```

Đăng tin không chờ AI (NFR-05): nếu Jina lỗi, tin vẫn được lưu, job thử lại sau.

## Luồng 2: đối chiếu nền

```mermaid
sequenceDiagram
  autonumber
  participant J as Job (mỗi 30s hoặc được đánh thức)
  participant DB as Postgres
  participant Jina
  participant N as Notification
  J->>DB: SELECT items WHERE embedding_status='pending'<br/>AND next_embed_at<=now() FOR UPDATE SKIP LOCKED LIMIT 5
  loop mỗi tin
    J->>Jina: embed chữ + ảnh nhỏ 512px (bỏ ảnh nếu danh mục nhạy cảm, BR-08)
    alt lỗi / 429
      J->>DB: attempts+1, next_embed_at = now + 2^attempts phút
    else thành công
      J->>DB: insert item_embeddings, embedding_status=done
      J->>DB: Lấy ứng viên loại đối ứng, đang open,<br/>lọc thô thời gian + bán kính 3 km, cosine max
      J->>J: MatchScorer.score() (hàm thuần)
      J->>DB: upsert match_candidates (score ≥ 0.45)
      J->>N: score ≥ 0.65 và chưa báo → thông báo cho chủ tin mất
    end
  end
  N->>DB: insert notifications
  N-)N: Sau commit: WS nếu online, không thì Expo Push
```

## Luồng 3: xác minh và bàn giao

```mermaid
sequenceDiagram
  autonumber
  actor O as Owner
  actor F as Finder
  participant S as Server
  O->>S: POST /items/{foundId}/claims {answer}
  S->>S: ClaimAccess + giới hạn thử (BR-05) → claim=submitted
  S-->>F: Thông báo "Có yêu cầu nhận"
  F->>S: GET /claims/{id} (thấy câu trả lời + đáp án của mình)
  F->>S: POST /claims/{id}/accept
  S->>S: TX: claim=accepted, item=claimed
  S-->>O: Thông báo "Được chấp nhận"; mở chat, lộ vị trí chính xác
  O->>S: POST /claims/{id}/messages (chat hẹn gặp)
  Note over O,F: Gặp mặt
  F->>S: POST /claims/{id}/handover-code
  S-->>F: mã 1 lần, hết hạn 10 phút → hiện QR
  O->>S: Quét QR → POST /claims/{id}/handover {code}
  S->>S: TX: kiểm tra hash, hạn, người quét = claimant<br/>claim=completed, item found + lost liên kết = returned,<br/>Finder +10 uy tín, các claim khác của tin → rejected
  S-->>O: Hoàn tất
  S-->>F: Hoàn tất
```
