# Phân công 4 người (đề xuất)

_Lập: 23/11/2026, theo `docs/tien-do.md` cùng ngày._

File này là **gợi ý ai làm việc nào**. Trạng thái thật vẫn chỉ ghi ở `docs/tien-do.md`: trước khi bắt tay vào việc nào, người làm vẫn nhận việc theo mục "Cách dùng" ở đó (đổi 🔵, commit `docs(tien-do): nhận <mã>`). Ai xong sớm thì lấy tiếp việc ⬜ của người đang chậm theo đúng quy tắc trong `tien-do.md`, không phải chờ.

Chia theo **lát dọc** (10 §10.1): mỗi người giữ một module từ server tới màn hình, để ít sửa file của nhau và tự giải thích được khi bảo vệ.

## Đã đến đâu

- **Xong và đã gộp `main`:** S01, S02, S04, S05, S10, P0-01 (migration lõi), P0-02 (đăng nhập server, JWT, `/me`), migration R03/R04 (chưa có code).
- **Dở:** S06 còn build development build trên máy thật (đang có sửa Kotlin + `expo-build-properties` + `eas.json` **chưa commit** trên máy Kiên); S07 còn deploy Render; S09 còn đặt biến `SERVER_URL`; S03 chờ các máy khác.
- **App:** mới có khung Expo, chưa có màn nào. **Server:** có auth, chưa có module `item`, `matching`, `claim`, `notification`.
- **Còn lại:** 46 việc (4 việc chuẩn bị, 11 P0, 16 P1, 2 P2, 6 hoàn thiện, 7 phát hành CH Play).
- Mục "Tình hình hiện tại" trong `tien-do.md` đang cũ (vẫn ghi P0-01, P0-02 chưa gộp), cần sửa lần cập nhật tới.

Lịch dưới đây giả định tuần 1 bắt đầu **Thứ Hai 23/11/2026**: tuần 1 = 23–27/11, tuần 2 = 30/11–04/12, tuần 3 = 07–11/12, điểm cắt P2 = Thứ Hai 07/12, 12:00.

## Tóm tắt

| Người                                                   | Mảng                                                   | Module server                        | Thư mục app                                                                                     |
| ------------------------------------------------------- | ------------------------------------------------------ | ------------------------------------ | ----------------------------------------------------------------------------------------------- |
| **Người 1** (gợi ý: Kiên, đang giữ Firebase/EAS/Render) | Nền tảng, đăng nhập, thông báo, phát hành              | `auth`, `notification`, `common`     | `src/lib/`, `src/stores/auth-store.ts`, `src/features/{auth,notifications}/`, `src/app/(auth)/` |
| **Người 2**                                             | Tin đăng (tạo, sửa, đọc), kiểm duyệt, hết hạn          | `item`, `moderation` (trừ chặn)      | `src/features/items/` (form, api, hooks), `src/app/items/new.tsx`, `src/app/admin/`             |
| **Người 3**                                             | Design system, bản đồ/danh sách, claim, bàn giao, chat | `claim`, `chat` (REST)               | `src/components/ui/`, `src/app/(tabs)/index.tsx`, `src/app/items/[id]/`, `src/features/claims/` |
| **Người 4**                                             | AI, đối chiếu, tìm kiếm, WebSocket, chính sách dữ liệu | `matching`, `common/WebSocketConfig` | `src/features/matching/`, `src/app/(tabs)/search.tsx`, `src/lib/ws-client.ts`                   |

Số việc: Người 1: 14 (nhiều việc là thao tác console, nhỏ) · Người 2: 9 · Người 3: 9 · Người 4: 11 · Chung: 3.

## Người 1: nền tảng, đăng nhập, thông báo, phát hành

| Tuần | Mã      | Việc                                                                                                            | Ghi chú                                                                                   |
| ---- | ------- | --------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| 1    | S06 ◆   | Commit sửa Kotlin (`expo-build-properties`, `eas.json`), build development build trên máy thật                  | **Làm đầu tiên ngày 1**: cả nhóm chờ để `git pull` + `npm ci` + build lại                 |
| 1    | S03     | Nhắc 3 người còn lại xóa `EXPO_PUBLIC_SUPABASE_*`, gom xác nhận                                                 |                                                                                           |
| 1    | S07 ◆   | Deploy Render (region Singapore), đo cold start, ghi U4–U6                                                      | Mốc tuần 1 phải chạy qua Render                                                           |
| 1    | S09     | Đặt biến repo `SERVER_URL`, xem CI chạy xanh trên một PR                                                        | Sau S07                                                                                   |
| 1    | P0-03 ◆ | App đăng nhập Google, đặt tên, `auth-store`, `api-client`, `query-client`, banner chờ server thức               | Chặn P0-07, P1-04, R04, H03; giải mã token thật để chốt `firebase.sign_in_provider` (U18) |
| 1    | R01 ◆   | Tạo tài khoản Play Console, xác minh danh tính, phí 25 USD                                                      | Xác minh có thể mất vài ngày: làm ngay                                                    |
| 2    | R04     | Màn đồng ý điều khoản + `POST /me/terms` + chặn ở `UserStatusFilter`                                            | **Server và app gộp cùng lúc**, không thì mọi user bị 403 `TERMS_NOT_ACCEPTED`            |
| 2    | P1-03 ◆ | Server thông báo: `notifications`, listener `AFTER_COMMIT`, `ExpoPushClient`, push token, cài đặt               | Cần P1-01 (Người 3); luôn push kể cả khi đang nối WS (S01)                                |
| 2    | P1-04   | App thông báo: đăng ký token, hộp thư, chạm mở đúng màn, cài đặt                                                |                                                                                           |
| 2    | P1-15   | Xóa tài khoản                                                                                                   | **Không được cắt** (D14)                                                                  |
| 2    | H03     | APK release: khóa ký, SHA release vào Firebase, `EXPO_PUBLIC_API_URL` trỏ Render                                | Làm sớm trong tuần 2 để mở R05                                                            |
| 2    | R05     | Bản AAB (`production` trong `eas.json` đổi sang `app-bundle`), Play App Signing, SHA của khóa Play vào Firebase | Hiện `production` đang để `buildType: apk` (thay đổi chưa commit), cần sửa ở đây          |
| 3    | R06 ◆   | Thử nghiệm kín ≥ 12 người × 14 ngày                                                                             | Xem rủi ro 1 ở dưới                                                                       |
| 3    | H04     | Màn đổi URL server ẩn, chạy server trên laptop cùng Wi-Fi                                                       |                                                                                           |

## Người 2: tin đăng, kiểm duyệt, hết hạn

| Tuần | Mã      | Việc                                                                                            | Ghi chú                                                                               |
| ---- | ------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| 1    | P0-05 ◆ | `POST /items` multipart, `LocationRounding`, giới hạn BR-09, validation                         | **Làm ngày 1**: chặn P0-06, P0-07, P0-10. Dùng lại `StorageClient` ở `common/storage` |
| 1    | P0-06 ◆ | Đọc/sửa tin, ký signed URL theo lô, DTO theo người xem; test X1–X6                              | Cần P0-09 (Người 3). Lọc `bbox` theo `public_location` (ghi chú P0-01)                |
| 1    | P0-07   | App đăng tin: form, ảnh, resize, vị trí, câu hỏi kín, bản nháp                                  | Viết luôn `src/features/items/api/` + `hooks/` cho Người 3 dùng ở P0-08               |
| 2    | P1-13   | Chống trùng: `DuplicateRule` + app hỏi "Vẫn đăng?"                                              | Nối tiếp form P0-07                                                                   |
| 2    | P1-10   | Báo cáo, tự chuyển `under_review`, admin khôi phục/gỡ/tranh chấp/khóa, màn admin; test X18, X22 | Nếu trễ: admin chỉ khóa tài khoản (10 §10.4)                                          |
| 2    | P1-11   | Ảnh nhạy cảm: không ký URL cho người không quyền, job bỏ ảnh; test X5                           | Cần P1-05 (Người 3), sửa cả `matching` (báo Người 4)                                  |
| 2    | P1-14   | Hết hạn tin và claim + chạy khi khởi động, test với `Clock` giả                                 | Cần P1-03 (Người 1), P1-05 (Người 3): cuối tuần 2                                     |
| 2    | P1-16   | Offline nhẹ: persist cache, giữ bản nháp                                                        | Sửa `src/lib/query-client.ts` của Người 1 (sau khi P0-03 gộp); cắt được nếu trễ       |
| 3    | P2-02   | Nhãn QR dán đồ (chỉ khi qua điểm cắt)                                                           | Cần `react-native-view-shot` + `expo-sharing`: thư viện native, hỏi nhóm              |

## Người 3: design system, bản đồ, claim, chat

| Tuần | Mã      | Việc                                                                                                             | Ghi chú                                                                                      |
| ---- | ------- | ---------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| 1    | P0-09   | Hàm thuần: `ItemStateMachine`, `ClaimStateMachine`, `ItemAccess`, `ClaimAccess`, `AttemptPolicy`, `HandoverCode` | **Làm ngày 1–2**: chặn P0-06 của Người 2. Tự tính tay ví dụ trước khi viết code              |
| 1    | P0-04   | Design system: `Button`, `TextField`, `Screen` (tải/lỗi/rỗng), `Card`, token trong `tailwind.config.js`          | Chặn P0-07, P0-08: gộp sớm, bản tối thiểu trước                                              |
| 1    | P1-01 ◆ | Migration P1 (8 bảng, 2 partial unique index của `claims`)                                                       | Chỉ cần P0-01, làm ngay tuần 1 để tuần 2 không chờ; nhờ một người khác đọc SQL               |
| 1    | P0-08   | App bản đồ + danh sách + chi tiết + tin của tôi                                                                  | Dùng `api/` + `hooks/` của Người 2; chia file trong `src/features/items/` để không đụng form |
| 2    | P1-05 ◆ | Server claim + test X9–X12, hai luồng `accept` song song chỉ một thành công                                      |                                                                                              |
| 2    | P1-06   | Bàn giao QR, `manual-complete`, `ReputationService`; test X13                                                    |                                                                                              |
| 2    | P1-07   | App claim: nút "Đây là đồ của tôi", danh sách/chi tiết, chấp nhận/từ chối, QR                                    | Chạy được bước 6–9 kịch bản demo                                                             |
| 2    | P1-09   | Chat: REST + giao diện; test X14, X15                                                                            | Cần P1-08 (Người 4)                                                                          |
| 2–3  | R03     | Chặn người dùng; test X24–X29                                                                                    | Lọc cả ở `item`, `matching`: báo Người 2, Người 4 trước khi sửa file của họ                  |

## Người 4: AI, đối chiếu, tìm kiếm, WebSocket

| Tuần | Mã      | Việc                                                                                             | Ghi chú                                                                        |
| ---- | ------- | ------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------ |
| 1    | P0-11   | `MatchScorer` + `MatchReasons`, test 2 ví dụ tính tay (0,776 và 0,320)                           | **Làm ngày 1**: không chờ ai                                                   |
| 1    | P0-10 ◆ | `GeminiEmbeddingClient`, `GroqVisionClient`, `EmbeddingJob`, `embedding_cache`                   | Cần P0-05 (Người 2); trong lúc chờ viết client + test với HTTP giả             |
| 1    | P0-12 ◆ | `POST /search` + màn tìm kiếm chữ/ảnh, `degraded=true` khi Gemini lỗi                            | Mốc tuần 1                                                                     |
| 1–2  | P0-13   | Chụp bộ mẫu 30 found + 30 lost có "bẫy", bộ đánh giá, lệnh `--seed`                              | Chụp ảnh bắt đầu ngay ngày 1, không cần code                                   |
| 2    | P1-08   | WebSocket: `WebSocketConfig`, xác thực `CONNECT`, `WsEventPublisher`, `ws-client`; test X20, X21 | Chỉ cần P0-02, làm đầu tuần 2 để Người 3 làm chat                              |
| 2    | P1-02 ◆ | Đối chiếu nền, `match_candidates`, màn "Món có thể khớp"; test X7                                | Cần P1-01 (Người 3)                                                            |
| 2    | P1-12   | Báo khớp chủ động; test X8                                                                       | Cần P1-03 (Người 1); sửa `src/app/items/[id]/index.tsx` của Người 3: báo trước |
| 2    | R02     | Chính sách quyền riêng tư + trang xóa tài khoản (GitHub Pages)                                   | Người hiểu rõ dữ liệu gửi Gemini/Groq nhất                                     |
| 3    | R07     | Điền Play Console: Data safety, xếp hạng nội dung, quyền, ảnh chụp, mô tả                        | Cần R02, R05                                                                   |
| 3    | H01     | Hiệu chỉnh `c0`, `c1`, trọng số; số top-5 cuối ≥ 70%                                             |                                                                                |
| 3    | P2-01   | Bản đồ nhiệt (chỉ khi qua điểm cắt)                                                              |                                                                                |

## Việc chung (tuần 3)

| Mã  | Việc                       | Cách chia                                                                                                                                               |
| --- | -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| H02 | Rà quyền theo ma trận 3.2  | Người **không** viết endpoint rà: Người 1 rà `claim`/`chat`, Người 3 rà `item`/`moderation`, Người 2 rà `matching`/WS, Người 4 rà `auth`/`notification` |
| H05 | Tập demo 2 lần + `pg_dump` | Cả nhóm; Người 1 giữ kịch bản và máy demo                                                                                                               |
| H06 | Báo cáo + khai báo dùng AI | Mỗi người viết phần module của mình; Người 1 ghép                                                                                                       |

## Thứ tự trong tuần 1 (đường găng)

```
Ngày 1: S06 build lại (N1) · P0-05 (N2) · P0-09 (N3) · P0-11 + chụp ảnh mẫu (N4) · R01, S07 (N1)
Ngày 2: P0-03 (N1) · P0-06 (N2, khi P0-09 gộp) · P0-04, P1-01 (N3) · P0-10 (N4, khi P0-05 gộp)
Ngày 3–5: P0-07 (N2) · P0-08 (N3) · P0-12 (N4) → mốc: đăng nhập → đăng tin → bản đồ → tìm ra đúng món qua Render
```

PR chặn người khác (P0-05, P0-09, P0-04, P1-01, P0-03, P1-05, P1-08) được ưu tiên duyệt trong ngày.

## Rủi ro cần cả nhóm biết

1. **R06 cần 14 ngày thử nghiệm kín liên tục** với ≥ 12 người. Nếu R05 xong cuối tuần 2 (04/12) thì sớm nhất 18/12 mới xin lên bản chính thức, **sau buổi demo**. Muốn kịp phải đẩy một bản AAB sớm (đăng nhập + đăng tin là đủ) ngay khi P0-03 xong, và gom đủ 12 người thử từ tuần 1.
2. Tuần 2 của Người 3 nặng nhất (claim + QR + chat + chặn). Ai xong trước giữa tuần 2 thì nhận P1-09 hoặc R03.
3. AGENTS.md ghi nhóm 6 người; bản này chia cho 4. Nếu thêm người, tách Người 1 (phát hành R01, R05–R07, H03, H04) và Người 3 (chat P1-09, R03) ra trước.
4. Không còn việc thêm thư viện nào được lên lịch ngoài P2-02; thêm gói native ở tuần 3 bắt cả nhóm build lại.
