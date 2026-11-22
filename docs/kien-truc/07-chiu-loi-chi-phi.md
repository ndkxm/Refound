# 7. Chịu lỗi và chi phí

Mọi hạn mức dưới đây là **giả định chưa kiểm chứng**; cách kiểm tra ở `12-dieu-chua-chac.md`.

## 7.1 Hạn mức và mức dùng dự kiến

| Dịch vụ            | Hạn mức free _(giả định)_                                                             | Dự kiến dùng (300 tin, 10 người)                                                            | Dư địa                                                          | Kiểm tra |
| ------------------ | ------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- | --------------------------------------------------------------- | -------- |
| Supabase Postgres  | 500 MB; tạm dừng sau 7 ngày không hoạt động                                           | < 20 MB                                                                                     | Rất lớn                                                         | U1       |
| Supabase Storage   | 1 GB lưu, ~5 GB băng thông/tháng                                                      | 300 tin × 4 ảnh × (~250 KB lớn + ~50 KB nhỏ) ≈ 360 MB tối đa; thực tế ~1,5 ảnh/tin ≈ 135 MB | Đủ                                                              | U1       |
| Render web service | 512 MB RAM, 0,1 CPU, ngủ sau 15 phút, 750 giờ/tháng                                   | 1 service                                                                                   | Đủ giờ; RAM là rủi ro                                           | U4, U5   |
| Jina               | ~10 triệu token (có thể không làm mới)                                                | 300 tin × (1,5 ảnh × ~4.000 + ~50 chữ) ≈ 1,8 triệu + thử nghiệm 0,24 triệu + tìm kiếm       | Đủ nếu ~4.000 token/ảnh; rủi ro nếu một ảnh tốn nhiều hơn nhiều | U2       |
| Groq               | Giới hạn theo phút/ngày cho từng model                                                | 0 ở phương án A; ~1 request/ảnh ở phương án B                                               | Đủ                                                              | U8       |
| Expo Push          | Miễn phí, giới hạn theo giây                                                          | Vài chục/ngày                                                                               | Rất lớn                                                         | —        |
| Firebase Auth      | Đăng nhập Google miễn phí; **SMS OTP có thể phải trả tiền hoặc cần gói Blaze có thẻ** | 10 người, mỗi người 1–2 lần OTP                                                             | Không rõ                                                        | U16      |
| GitHub Actions     | Miễn phí cho repo public; 2.000 phút/tháng repo private                               | CI ~5 phút × ~100 lần + cron 1 phút/ngày                                                    | Đủ                                                              | —        |

## 7.2 Giảm số lần gọi AI

| Cách                                                                           | Ở đâu                             |
| ------------------------------------------------------------------------------ | --------------------------------- |
| Embed **một lần** khi tạo/sửa tin; lưu vector, không bao giờ embed lại khi đọc | `item_embeddings`                 |
| Embed ảnh nhỏ 512px (giả định 1 ô = 1 đơn vị token), không gửi ảnh lớn         | App resize; job dùng `thumb_path` |
| Cache vector câu truy vấn theo hash                                            | `embedding_cache`                 |
| Không gửi ảnh danh mục nhạy cảm (BR-08)                                        | Job                               |
| Bộ đánh giá lưu vector vào file, chạy lại không gọi API                        | `eval/cache-*.json`               |
| Dữ liệu demo nạp kèm vector có sẵn                                             | Lệnh seed                         |
| Không gọi Groq ở phương án A                                                   | ADR-02                            |

## 7.3 Hàng đợi thử lại (NFR-05)

Không có hàng đợi riêng: cột `embedding_status`, `embedding_attempts`, `next_embed_at` trên `items` **là** hàng đợi (ADR-01).

| Tình huống                      | Xử lý                                                                                                                                                                                |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Jina lỗi mạng / 5xx             | `attempts+1`, `next_embed_at = now() + 2^(attempts−1) phút`; tối đa 6 lần → `failed`                                                                                                 |
| Jina 429 / 402 (hết hạn mức)    | Như trên + `AiCircuit.pauseUntil = now()+5 phút`: job và `/search` bỏ qua Jina tới lúc đó                                                                                            |
| Server bị Render tắt giữa chừng | Transaction chưa commit thì rollback; tin vẫn `pending`; lần thức sau job `@Scheduled` vét lại                                                                                       |
| Hết hẳn token Jina              | Đổi `EMBEDDING_PROVIDER=gemini` (phương án B, cùng 768 chiều, interface `EmbeddingClient`), đặt lại `pending` cho mọi tin. **Không trộn** vector hai model: cột `model` cho phép lọc |
| Expo Push lỗi                   | Thông báo đã nằm trong `notifications`; app thấy trong hộp thông báo. Không thử lại push (shortcut, đủ cho demo)                                                                     |
| Storage lỗi khi đăng tin        | Trả 503, không ghi DB; app giữ bản nháp (NFR-08) và cho thử lại                                                                                                                      |

## 7.4 Giới hạn tần suất

Một instance duy nhất, nên đếm trong DB hoặc bộ nhớ là đủ, không cần Redis hay Bucket4j.

| Luật                     | Cách đếm                                                                                                    |
| ------------------------ | ----------------------------------------------------------------------------------------------------------- |
| BR-09: 10 tin/người/24h  | `count(*) from items where author_id=? and created_at > now()-24h`                                          |
| BR-05: số lần bị từ chối | `count(*) from claims where claimant_id=? and found_item_id=? and status='rejected'` so với `AttemptPolicy` |
| Đăng nhập                | Không cần ở server: không có mật khẩu để dò; Firebase tự giới hạn số lần gửi OTP                            |
| Tin nhắn: 30/phút/người  | Bộ nhớ, như trên                                                                                            |
| Kích thước               | Ảnh ≤ 1 MB, request ≤ 6 MB (`spring.servlet.multipart`)                                                     |

## 7.5 Render free (Q7)

**Server:**

| Việc              | Cấu hình                                                                                                                            |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| Image             | Multi-stage Dockerfile: `maven:3-eclipse-temurin-21` build → `eclipse-temurin:21-jre` chạy                                          |
| JVM               | `-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m` |
| Spring            | `spring.jpa.open-in-view=false`, Hikari `maximum-pool-size=3`, Tomcat `threads.max=20`, không bật devtools, actuator chỉ nếu cần    |
| Khởi động nhanh   | Đo trước; nếu > 60 s mới thử CDS của Spring Boot 3.3+ hoặc `spring.main.lazy-initialization=true`                                   |
| Việc sau khi thức | Job `@Scheduled` chạy ngay lần đầu (vét embedding, hết hạn)                                                                         |

**App:**

| Việc           | Cách                                                                                                                                 |
| -------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| Lần gọi đầu    | Khi mở app gọi `GET /health` timeout 90 s; sau 3 s chưa có phản hồi thì hiện banner "Máy chủ đang khởi động, có thể mất tới 1 phút…" |
| TanStack Query | `retry`: 3 lần, lùi 2/4/8 s, chỉ với lỗi mạng, 502, 503, 504; không retry 4xx. Timeout request thường 30 s                           |
| Ghi dữ liệu    | Không tự retry POST (tránh tạo hai tin); hiện lỗi và nút "Thử lại", form giữ nguyên                                                  |
| WebSocket      | Tự nối lại lùi dần 1→30 s; khi nối lại hoặc app về foreground thì `invalidateQueries` cho chat/claim đang mở                         |

**Giữ dịch vụ không bị ngủ đông / tạm dừng:**

- GitHub Actions cron **1 lần/ngày** gọi `GET /health?db=true`: chạy job hết hạn (BR-07) và chạm DB (tránh Supabase tạm dừng). Đây không phải ping liên tục để giữ Render thức. _(giả định chính sách Render cho phép, U6)_.
- Trước buổi demo 10 phút: mở `/health` bằng trình duyệt, chờ `db: up`, mở app trên hai máy.
- **Phương án dự phòng demo:** chạy server trên laptop (`./mvnw spring-boot:run`, profile `local` vẫn nối Supabase), hai điện thoại cùng Wi-Fi; trong app có màn ẩn (nhấn giữ logo 3 s ở màn đăng nhập) để đổi URL server, lưu bằng `expo-secure-store`.

## 7.6 Sao lưu

Supabase free không đảm bảo bản sao lưu tải về được _(giả định)_. Trước demo: `pg_dump --schema=refound` ra máy một người. Dữ liệu demo luôn dựng lại được bằng lệnh seed.
