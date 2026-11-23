# Tiến độ Refound

> **Người và AI đọc file này đầu tiên** mỗi khi bắt đầu một phiên làm việc. Đây là nơi duy nhất ghi: đã làm đến đâu, ai đang làm gì, việc nào làm được tiếp theo. Thiết kế chi tiết nằm ở `docs/kien-truc/`, yêu cầu ở `docs/yeu-cau-nghiep-vu.md`, quy ước ở `AGENTS.md`.

## Cách dùng

**Ký hiệu trạng thái:** ⬜ Chưa làm · 🔵 Đang làm · 🔴 Bị chặn · ✅ Xong · ❓ Chờ nhóm quyết · ✂ Cắt

**Chọn việc tiếp theo:**

1. Lấy việc ⬜ có mọi việc ở cột "Cần trước" đã ✅.
2. Cột "File chính" không trùng với việc nào đang 🔵 (tránh hai người sửa cùng file).
3. Ưu tiên việc nằm trên đường găng: các việc có ◆ ở cột Mã.

**Nhận việc:** đổi trạng thái thành 🔵, điền `Tên · feature/<nhánh>` vào cột cuối, rồi commit **chỉ file này** thẳng lên `main` với message `docs(tien-do): nhận <mã việc>` để cả nhóm thấy ngay. Đây là ngoại lệ duy nhất của quy tắc "gộp qua PR" (quyết định D9) và chỉ áp dụng cho file này, cho việc đổi trạng thái 🔵 / 🔴 / ✂ và cột người nhận. Trước khi commit chạy `git pull --rebase` để không ghi đè việc người khác vừa nhận; nếu xung đột ở cùng một dòng thì người commit sau chọn việc khác. Đánh dấu ✅ vẫn làm trong PR của việc đó.

**Làm xong:** trong chính PR của việc đó:

- đổi trạng thái thành ✅, thêm link PR vào cột cuối;
- nếu có điều người sau cần biết (làm khác thiết kế, còn nợ gì, lệnh cần chạy), thêm mục `### <mã việc>` vào phần **Ghi chú theo việc**;
- thêm một dòng vào **Nhật ký**;
- nếu việc làm thay đổi tình hình chung, sửa mục **Tình hình hiện tại**.

Một việc chỉ được ✅ khi đạt cột "Kết quả cần có" **và** đủ điều kiện "Một việc được coi là xong" trong `AGENTS.md` (chạy trên máy Android thật nếu có UI, `npm run check` qua, `./mvnw verify` qua nếu có sửa server, PR đã gộp).

**Bị chặn:** đổi thành 🔴, ghi lý do ngắn trong ngoặc ở cột cuối, báo nhóm. Kẹt quá 30 phút với AI ở cùng một lỗi thì làm bước này (AGENTS.md).

**Dành cho AI:**

- Trước khi viết code cho một việc: đọc các mục ở cột "Thiết kế" và phần Ghi chú của các việc ở cột "Cần trước".
- Không tự nhận việc khác ngoài việc người dùng giao; không đổi trạng thái việc của người khác.
- Không đánh dấu ✅ khi chưa chạy lệnh kiểm tra và chưa thấy kết quả thật. Nếu làm khác thiết kế, ghi rõ trong Ghi chú và hỏi người dùng có cần sửa tài liệu thiết kế không.
- Ngày tháng ghi dạng `dd/mm/yyyy`.

## Tình hình hiện tại

_Cập nhật: 23/11/2026_

- **Giai đoạn:** chuẩn bị (trước tuần 1). Chưa có code app ngoài khung Expo; `server/` có khung Spring rỗng + `/api/v1/health` (S07, chưa commit).
- **Đã có:** khung Expo (commit `aea9c46`), tài liệu yêu cầu v0.2, thiết kế kiến trúc trong `docs/kien-truc/`; BRD, đề cương, `AGENTS.md`, `docs/setup.md` đã sửa theo các quyết định D1–D4 (S02). Tất cả **chưa gộp `main`**.
- **Đang chờ:** commit + PR (chưa làm, chờ Kiên bảo). Việc cần người làm: deploy Render (S07) rồi đặt biến `SERVER_URL` (S09); S03 ở các máy còn lại; build lại development build trên máy mỗi người (S06).
- **Làm được ngay:** P0-02 (khi PR P0-01 gộp), P0-09, P0-11 (hàm thuần, chỉ cần S07), P0-04 (S06 đã thêm thư viện).
- **Rủi ro:** mọi giả định U đã được chấp nhận không kiểm (D13), nên lỗi sẽ lộ lúc làm việc: U5 khi deploy Render, U13 + U17 khi build development build, U8 (model Groq preview) ở P0-10.

## Quyết định

| Mã  | Nội dung                                                                                                                                                                            | Trạng thái                              | Ngày                         | Chi tiết                              |
| --- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------- | ---------------------------- | ------------------------------------- |
| D1  | Đăng nhập bằng Google **hoặc** số điện thoại + OTP qua Firebase Auth (phần SĐT bỏ ở D10); server kiểm tra Firebase ID token rồi phát hành JWT riêng; không lưu email, SĐT, mật khẩu | ✅ Chốt                                 | 22/11/2026                   | ADR-05                                |
| D2  | Ảnh đọc bằng signed URL 1 giờ do server cấp                                                                                                                                         | ✅ Chốt                                 | 22/11/2026                   | ADR-06                                |
| D3  | Không chia việc cố định theo người; nhận việc theo file này                                                                                                                         | ✅ Chốt                                 | 22/11/2026                   | ADR-12                                |
| D4  | Cắt NFC (FR-TAG-02) và geofence (FR-GEO-01)                                                                                                                                         | ✅ Chốt                                 | 22/11/2026                   | van-de V19                            |
| D5  | Model embedding (Q3): Gemini `gemini-embedding-2` 768 chiều; bỏ Jina                                                                                                                | ✅ Chốt (Kiên)                          | 23/11/2026                   | ADR-02                                |
| D6  | Server Spring chạy được trên Render free (Q7)                                                                                                                                       | ❓ Chờ kết quả S07                      | ✅ Chốt (Kiên, không chờ đo) | 23/11/2026                            |
| D7  | Bật SMS OTP thật hay chỉ dùng số thử khi demo                                                                                                                                       | ✂ Không còn (D10)                       |                              | van-de V24                            |
| D9  | Được commit thẳng `docs/tien-do.md` lên `main` khi nhận việc hoặc báo bị chặn (chỉ file này)                                                                                        | ✅ Chốt                                 | 22/11/2026                   | Mục "Cách dùng"                       |
| D8  | Các ADR còn ghi "Đề xuất"                                                                                                                                                           | ✅ Chốt (trừ ADR-02, ADR-09 chờ D5, D6) | 22/11/2026                   | `docs/kien-truc/11-adr.md`            |
| D10 | Bỏ đăng nhập số điện thoại + OTP, chỉ Google; vẫn đi qua Firebase Auth (không chuyển sang kiểm tra thẳng Google ID token)                                                           | ✅ Chốt (Kiên)                          | 22/11/2026                   | ADR-05                                |
| D11 | Mô tả ảnh bằng Groq `qwen/qwen3.8-27b` (vision), ghép vào chữ trước khi embed; không hiện cho người dùng; Groq lỗi thì bỏ qua                                                       | ✅ Chốt (Kiên)                          | 23/11/2026                   | ADR-02                                |
| D12 | Bỏ S08 (không thử nghiệm model trước); bộ mẫu và số top-5 làm ở P0-13                                                                                                               | ✅ Chốt (Kiên)                          | 23/11/2026                   | ADR-02                                |
| D13 | Chấp nhận mọi giả định U còn lại mà không kiểm trước; sai thì theo phương án dự phòng                                                                                               | ✅ Chốt (Kiên)                          | 23/11/2026                   | `docs/kien-truc/12-dieu-chua-chac.md` |

## Việc

### Chuẩn bị (tuần 1, ngày 1–2)

| Mã    | Việc                                                                                                                                        | Cần trước                             | File chính                                                  | Kết quả cần có                                                                                                                          | Thiết kế         | TT  | Người · nhánh / PR                                                                   |
| ----- | ------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- | ----------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- | ---------------- | --- | ------------------------------------------------------------------------------------ |
| S01   | Nhóm đọc thiết kế, quyết các mục ❓ (D4, D8)                                                                                                | —                                     | `docs/kien-truc/*`                                          | Mỗi ADR "Đề xuất" đổi thành "Chốt" hoặc ghi phương án thay thế; bảng Quyết định cập nhật                                                | README kiến trúc | ✅  | AI (Claude) theo yêu cầu của Kiên · chưa commit                                      |
| S02   | Sửa BRD, `AGENTS.md`, `docs/setup.md` theo quyết định mới                                                                                   | S01                                   | `docs/yeu-cau-nghiep-vu.md`, `AGENTS.md`, `docs/setup.md`   | Hết các mâu thuẫn V1, V2, V9, V21, V23; `AGENTS.md` bỏ cột "Phụ trách" theo TV và thêm module `moderation`                              | van-de           | ✅  | AI (Claude) · chưa commit                                                            |
| S03   | Xóa `EXPO_PUBLIC_SUPABASE_*` khỏi `.env` mọi máy                                                                                            | —                                     | `.env` (local)                                              | Mỗi người xác nhận trong nhóm; `npm run check` không còn in hai biến này                                                                | van-de V22       | 🔵  | AI (Claude): xong máy Kiên; chờ các máy khác xác nhận                                |
| S04   | Thiết lập Supabase: project, extension `vector` + `postgis`, bucket private `item-images`, schema `refound` không nằm trong exposed schemas | —                                     | `docs/setup.md`                                             | Kết nối bằng chuỗi session pooler từ máy local thành công; ghi kết quả U1, U7                                                           | ADR-10, 12 U1 U7 | ✅  | Kiên                                                                                 |
| S05   | Thiết lập Firebase Auth: bật Google, khai SHA-1/SHA-256 khóa debug, lấy Web client ID                                                       | —                                     | (console Firebase), `docs/setup.md`                         | Có `google-services.json` mới và Web client ID; ghi kết quả U18                                                                         | ADR-05           | ✅  | Kiên · U18 còn bước giải mã token thật (P0-03)                                       |
| S06 ◆ | Thêm thư viện một đợt (8.1) + build lại development build; bỏ `/supabase/` trong `jest.testPathIgnorePatterns`                              | S05                                   | `package.json`, `app.config.ts`                             | Development build mới trên máy thật: hiện bản đồ MapLibre, mở camera, đăng nhập Google thử được; `npm run check` qua; ghi U11, U13, U17 | 08 §8.1          | 🔵  | AI (Claude) · chưa commit; còn build lại development build trên máy thật (người làm) |
| S07 ◆ | Dựng server Spring rỗng (Maven, Java 21, `GET /api/v1/health`), Dockerfile, deploy Render; đo cold start và RAM                             | S04                                   | `server/pom.xml`, `server/Dockerfile`, `server/.../common/` | URL Render trả `/api/v1/health?db=true` = up; số đo (thời gian thức, RSS, 6 request đồng thời) ghi vào Ghi chú; ghi U4, U5, U6          | ADR-09, 07 §7.5  | 🔵  | AI (Claude) · code xong, chưa commit; còn deploy Render (người làm)                  |
| S08   | Chụp bộ ảnh mẫu (30 found, 30 lost, có "bẫy") + đo độ chính xác `gemini-embedding-2`                                                        | — (chụp ảnh); S07 nếu chạy bằng JUnit | `server/src/test/resources/eval/`                           | `dataset.json` + ảnh; bảng top-1, top-5, MRR, độ trễ, phân bố cosine (gợi ý `c0`, `c1`); ghi U2, U3, U10                                | 04 §4.6, ADR-02  | ✂   | Kiên bỏ (D12, 23/11/2026)                                                            |
| S09   | CI GitHub Actions: `npm run check` + `./mvnw verify`; cron 1 lần/ngày gọi `/api/v1/health?db=true`                                          | S07                                   | `.github/workflows/`                                        | PR hiện check xanh/đỏ; cron chạy thành công một lần                                                                                     | 09 §9.2, 07 §7.5 | 🔵  | AI (Claude) · chưa commit; còn đặt biến `SERVER_URL`, xem PR chạy                    |
| S10   | Nền test server: Testcontainers, fixture `TestUsers`, JWKS giả cho X23                                                                      | S07, P0-01                            | `server/src/test/java/.../support/`                         | Một test IT chạy migration P0 trên container và qua; ghi U9                                                                             | 09 §9.2          | ✅  | AI (Claude) · chưa commit; `TestUsers` chuyển sang P0-02                             |

### P0 (tuần 1)

| Mã      | Việc                                                                                                                                                                                                    | Cần trước           | File chính                                                                                             | Kết quả cần có                                                                                                                                                                  | Thiết kế                | TT  | Người · nhánh / PR               |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------- | ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------- | --- | -------------------------------- |
| P0-01 ◆ | Migration P0                                                                                                                                                                                            | S04, S07            | `db/migration/V…__core_p0.sql`                                                                         | Flyway chạy được trên Supabase và Testcontainers; `categories` có 14 dòng                                                                                                       | 02 §2.4                 | ✅  | Kiên · `feature/p0-01-migration` |
| P0-02 ◆ | Server đăng nhập: `POST /auth/firebase`, `JwtIssuer`, `SecurityConfig`, `UserStatusFilter`, `GET/PATCH /me`, `PUT /me/avatar`, `GET /users/{id}`, xử lý lỗi `ProblemDetail`                             | P0-01, S05          | `server/.../auth/`, `common/SecurityConfig`, `common/ApiExceptionHandler`                              | Test X17, X19, X23 xanh; token hết hạn → 401; fixture `TestUsers` (chuyển từ S10); test X23 dùng `FakeFirebaseIdp`                                                              | ADR-05, 03, 06 §6.2     | ⬜  |                                  |
| P0-03 ◆ | App đăng nhập: nút Google, đặt tên lần đầu, `auth-store`, `api-client`, `query-client` (retry 7.5), banner chờ server thức                                                                              | S06, P0-02          | `src/app/(auth)/*`, `src/features/auth/`, `src/lib/`, `src/stores/auth-store.ts`                       | Đăng nhập bằng Google trên máy thật; giải mã một ID token thật xác nhận `iss`, `aud`, `firebase.sign_in_provider` (U18); `is-retryable.test.ts` qua; banner hiện khi server ngủ | 08 §8.1, 07 §7.5        | ⬜  |                                  |
| P0-04   | Design system tối thiểu: `Button`, `TextField`, `Screen` (đang tải / lỗi có nút thử lại / rỗng), `Card`; màu, cỡ chữ trong `tailwind.config.js`                                                         | S06                 | `src/components/ui/`, `tailwind.config.js`                                                             | Màn khác dùng được; vùng chạm ≥ 44dp; đủ tương phản sáng/tối                                                                                                                    | AGENTS.md               | ⬜  |                                  |
| P0-05 ◆ | Server tạo tin: `POST /items` multipart, `SupabaseStorageClient`, `LocationRounding`, giới hạn BR-09, validation                                                                                        | P0-01, P0-02        | `server/.../item/`                                                                                     | `LocationRoundingTest` dùng ví dụ 2.3; ảnh lên Storage thật; tin thứ 11 trong 24h → 429                                                                                         | 02 §2.3, ADR-06, ADR-14 | ⬜  |                                  |
| P0-06 ◆ | Server đọc/sửa tin: `GET /items` (bbox, lọc), `GET /items/{id}`, `/me/items`, `PATCH`, `close`, `mark-found`, `renew`, `DELETE`, `/items/{id}/secret`, ký signed URL theo lô, DTO theo người xem        | P0-05, P0-09        | `server/.../item/`                                                                                     | Test X1–X6 xanh                                                                                                                                                                 | 03, 05 §5.1, 06         | ⬜  |                                  |
| P0-07   | App đăng tin: form found/lost, chụp/chọn ảnh, resize 1024/512, vị trí + bán kính, câu hỏi kín, lưu bản nháp                                                                                             | P0-03, P0-04, P0-05 | `src/features/items/`, `src/app/items/new.tsx`, `src/app/(tabs)/post.tsx`, `src/stores/draft-store.ts` | Đăng tin nhặt được dưới 1 phút trên máy thật; `item-form-schema.test.ts`, `resize-plan.test.ts` qua                                                                             | 01 luồng 1, ADR-14      | ⬜  |                                  |
| P0-08   | App bản đồ + danh sách + chi tiết + tin của tôi                                                                                                                                                         | P0-04, P0-06        | `src/app/(tabs)/index.tsx`, `src/app/items/[id]/`, `src/features/items/`                               | Đủ ba trạng thái; tin `found` hiện vị trí làm tròn; `format-distance.test.ts` qua                                                                                               | FR-ITEM-03, 04          | ⬜  |                                  |
| P0-09   | Hàm thuần nghiệp vụ: `ItemStateMachine`, `ClaimStateMachine`, `ItemAccess`, `ClaimAccess`, `AttemptPolicy`, `HandoverCode`                                                                              | S07                 | `server/.../{item,claim}/domain/`                                                                      | JUnit phủ mọi dòng bảng 5.1, 5.2, 5.3 và vài chuyển trạng thái sai                                                                                                              | 05, 03 §3.3             | ⬜  |                                  |
| P0-10 ◆ | Embedding: `EmbeddingClient` + `GeminiEmbeddingClient`, `VisionClient` + `GroqVisionClient` (`ai_caption`), `EmbeddingJob` (hàng đợi trên `items`, lùi dần, tạm dừng 5 phút khi 429), `embedding_cache` | P0-05               | `server/.../matching/`                                                                                 | Tin mới → `embedding_status=done` trong dưới 1 phút khi server thức; giả lập Gemini lỗi → `pending` và thử lại; giả lập Groq lỗi → vẫn `done`, `ai_caption` null                | 04 §4.1, 4.4, ADR-01    | ⬜  |                                  |
| P0-11   | `MatchScorer` + `MatchReasons`                                                                                                                                                                          | S07                 | `server/.../matching/domain/`                                                                          | `MatchScorerTest` có 2 ví dụ tính tay ở 4.2 (0,776 và 0,320) + các ca biên                                                                                                      | 04 §4.2                 | ⬜  |                                  |
| P0-12 ◆ | Tìm kiếm: `POST /search` + màn tìm kiếm (chữ, ảnh)                                                                                                                                                      | P0-08, P0-10, P0-11 | `server/.../matching/`, `src/app/(tabs)/search.tsx`, `src/features/matching/`                          | Kết quả có điểm + lý do; Gemini lỗi → `degraded=true` và app báo; `score-label.test.ts` qua                                                                                     | 04 §4.3                 | ⬜  |                                  |
| P0-13   | Chụp bộ ảnh mẫu (30 found, 30 lost, có "bẫy", 4.6) + bộ đánh giá + lệnh seed                                                                                                                            | P0-10, P0-11        | `server/src/test/resources/eval/`, lệnh `--seed`                                                       | `./mvnw test -Dgroups=eval` in top-1, top-5, MRR; `--seed` nạp 60 tin kèm vector có sẵn                                                                                         | 04 §4.6, 09 §9.3        | ⬜  |                                  |

**Mốc tuần 1:** đăng nhập → đăng tin có ảnh → thấy trên bản đồ → tìm bằng chữ ra đúng món, trên máy thật qua server Render; có số top-5 đầu tiên.

### P1 (tuần 2)

| Mã      | Việc                                                                                                                                     | Cần trước           | File chính                                                                                      | Kết quả cần có                                                                             | Thiết kế         | TT  | Người · nhánh / PR |
| ------- | ---------------------------------------------------------------------------------------------------------------------------------------- | ------------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ | ---------------- | --- | ------------------ |
| P1-01 ◆ | Migration P1: `match_candidates`, `claims`, `handovers`, `chat_messages`, `notifications`, `push_tokens`, `reports`, `reputation_events` | P0-01               | `db/migration/`                                                                                 | Có hai partial unique index của `claims` như 2.4                                           | 02 §2.2          | ⬜  |                    |
| P1-02 ◆ | Đối chiếu nền: chấm cặp, upsert `match_candidates`, `GET /items/{id}/matches`, `dismiss`, màn "Món có thể khớp"                          | P0-10, P0-11, P1-01 | `server/.../matching/`, `src/app/items/[id]/matches.tsx`                                        | Ứng viên ≥ 0,45 được lưu; test X7 xanh                                                     | 04 §4.4          | ⬜  |                    |
| P1-03 ◆ | Server thông báo: bảng `notifications`, listener `AFTER_COMMIT`, `ExpoPushClient`, push token, cài đặt từng loại, API đọc/đánh dấu       | P1-01               | `server/.../notification/`                                                                      | Test X16 xanh; transaction rollback thì không push                                         | ADR-08           | ⬜  |                    |
| P1-04   | App thông báo: đăng ký push token, hộp thư, chạm thông báo mở đúng màn, màn cài đặt thông báo                                            | P0-03, P1-03        | `src/features/notifications/`, `src/app/(tabs)/inbox.tsx`, `src/app/settings/notifications.tsx` | Push đến máy thật khi app đóng (U14); `notification-route.test.ts` qua                     | FR-NOTI-01..03   | ⬜  |                    |
| P1-05 ◆ | Server claim: `question`, `submit`, `accept`, `reject`, `withdraw`, `cancel`, `dispute`, `GET /claims`, `GET /claims/{id}`               | P0-06, P0-09, P1-01 | `server/.../claim/`                                                                             | Test X9–X12 xanh; hai luồng cùng `accept` → chỉ một thành công                             | 05 §5.2          | ⬜  |                    |
| P1-06   | Server bàn giao: `handover-code`, `handover` (quét QR), `manual-complete`, `ReputationService` (+10 khi quét QR)                         | P1-05               | `server/.../claim/`, `server/.../moderation/ReputationService`                                  | Test X13 xanh; mã dùng lần 2 hoặc quá 10 phút → 409                                        | 05 C5b–C6b       | ⬜  |                    |
| P1-07   | App claim: nút "Đây là đồ của tôi", danh sách và chi tiết claim, chấp nhận/từ chối, hiện QR, quét QR                                     | P0-08, P1-05, P1-06 | `src/features/claims/` (trừ chat), `src/app/claims/`                                            | `claim-actions.test.ts`, `parse-handover-qr.test.ts` qua; chạy được bước 6–9 kịch bản demo | 09 §9.3          | ⬜  |                    |
| P1-08   | WebSocket: `WebSocketConfig`, xác thực `CONNECT`, `WsEventPublisher`, `ws-client` ở app                                                  | P0-02               | `server/.../chat/`, `common/WebSocketConfig`, `src/lib/ws-client.ts`                            | Test X20, X21 xanh; chạy qua Render (U6)                                                   | 06 §6.3, ADR-07  | ⬜  |                    |
| P1-09   | Chat: `GET/POST /claims/{id}/messages` + giao diện chat                                                                                  | P1-05, P1-08        | `server/.../chat/`, `src/features/claims/components/chat-*`                                     | Test X14, X15 xanh; tin đến dưới 2 giây khi hai máy online                                 | FR-CLAIM-03      | ⬜  |                    |
| P1-10   | Kiểm duyệt: báo cáo, tự chuyển `under_review`, admin khôi phục/gỡ/xử lý tranh chấp/khóa tài khoản, màn admin                             | P0-06, P1-01        | `server/.../moderation/`, `src/app/admin/`                                                      | Test X18, X22 xanh; 3 báo cáo từ 3 người → tin bị ẩn                                       | 05 I10–I12, C8   | ⬜  |                    |
| P1-11   | Ảnh nhạy cảm (FR-ITEM-07, BR-08): không ký URL cho người không có quyền, job bỏ ảnh, tin `lost` nhạy cảm không có ảnh                    | P0-06, P0-10, P1-05 | `server/.../item/`, `server/.../matching/`                                                      | Test X5 xanh; job không gửi ảnh nhạy cảm cho Gemini (test)                                 | 03 §3.2, 04 §4.1 | ⬜  |                    |
| P1-12   | Báo khớp chủ động (FR-MATCH-06)                                                                                                          | P1-02, P1-03        | `server/.../matching/`, `src/app/items/[id]/index.tsx`                                          | Test X8 xanh; chủ tin mất nhận thông báo                                                   | 04 §4.4          | ⬜  |                    |
| P1-13   | Chống trùng (FR-MATCH-04)                                                                                                                | P0-05, P0-07        | `server/.../item/domain/DuplicateRule`, form đăng tin                                           | `DuplicateRuleTest` qua; app hỏi "Vẫn đăng?" và gửi lại với `force=true`                   | 04 §4.5          | ⬜  |                    |
| P1-14   | Hết hạn tin và claim (BR-07, C9) + chạy khi khởi động                                                                                    | P1-03, P1-05, S09   | `server/.../item/`, `server/.../claim/`                                                         | Test tích hợp với `Clock` giả: cảnh báo trước 3 ngày, hết hạn sau 30 ngày                  | 05 I8, I8a, C9   | ⬜  |                    |
| P1-15   | Xóa tài khoản (FR-AUTH-04)                                                                                                               | P0-02, P1-01        | `server/.../auth/`, `src/app/(tabs)/profile.tsx`                                                | Như van-de V16                                                                             | 06 §6.2          | ⬜  |                    |
| P1-16   | Offline nhẹ (NFR-08): persist cache, giữ bản nháp                                                                                        | P0-08               | `src/lib/query-client.ts`                                                                       | Tắt mạng vẫn xem được danh sách đã tải; gửi lỗi không mất nội dung                         | 08 §8.1          | ⬜  |                    |

**Mốc tuần 2:** chạy trọn kịch bản demo 9.3 trên 2 máy; mọi test X1–X23 xanh.

**Điểm cắt P2:** Thứ Hai tuần 3, 12:00. Chưa đạt mốc tuần 2 → mọi việc P2 chuyển ✂.

### P2 (tuần 3, chỉ khi qua điểm cắt)

| Mã    | Việc                                   | Cần trước             | File chính                                 | Kết quả cần có                                    | Thiết kế   | TT  | Người · nhánh / PR |
| ----- | -------------------------------------- | --------------------- | ------------------------------------------ | ------------------------------------------------- | ---------- | --- | ------------------ |
| P2-01 | Bản đồ nhiệt (FR-HEAT-01)              | Điểm cắt              | `GET /stats/heatmap`, lớp heatmap MapLibre | Chỉ hiện ô có ≥ 3 tin                             | 06 §6.2 P2 | ⬜  |                    |
| P2-02 | Nhãn QR dán đồ (FR-TAG-01)             | P2-01 hoặc người rảnh | bảng `tags`, `/tags/*`                     | Quét nhãn → chủ nhận tin nhắn, không lộ danh tính | 10 §10.4   | ⬜  |                    |
| P2-03 | Nhãn NFC (FR-TAG-02)                   | D4                    |                                            |                                                   | van-de V19 | ✂   |                    |
| P2-04 | Nhắc quên đồ bằng geofence (FR-GEO-01) | D4                    |                                            |                                                   | van-de V19 | ✂   |                    |

### Hoàn thiện (tuần 3)

| Mã  | Việc                                                                                        | Cần trước | File chính                    | Kết quả cần có                                       | Thiết kế         | TT  | Người · nhánh / PR |
| --- | ------------------------------------------------------------------------------------------- | --------- | ----------------------------- | ---------------------------------------------------- | ---------------- | --- | ------------------ |
| H01 | Hiệu chỉnh `c0`, `c1`, trọng số trên bộ mẫu                                                 | P0-13     | `MatchWeights`                | Số top-5 cuối ≥ 70% (G1) hoặc ghi rõ vì sao chưa đạt | 04 §4.6          | ⬜  |                    |
| H02 | Rà quyền theo ma trận 3.2 (người **không** viết endpoint đọc lại từng endpoint)             | P1 xong   | toàn server                   | Danh sách endpoint đã rà + lỗi tìm được              | 03               | ⬜  |                    |
| H03 | APK phát hành: khóa ký, SHA của khóa release vào Firebase, `EXPO_PUBLIC_API_URL` trỏ Render | P0-03     | `app.config.ts`, `eas.json`   | APK cài trên 2 máy, đăng nhập Google được            | ADR-05           | ⬜  |                    |
| H04 | Phương án dự phòng demo: màn đổi URL server ẩn, chạy server trên laptop cùng Wi-Fi          | P0-03     | `src/app/settings/server.tsx` | Chuyển sang server laptop trong dưới 1 phút          | 07 §7.5          | ⬜  |                    |
| H05 | Tập kịch bản demo 2 lần; `pg_dump --schema=refound` trước buổi demo                         | H03, H04  |                               | Hai lần liền không lỗi, mỗi lần 5–7 phút             | 09 §9.3, 07 §7.6 | ⬜  |                    |
| H06 | Báo cáo đồ án + khai báo dùng AI                                                            | —         |                               | Theo quy định môn                                    | AGENTS.md        | ⬜  |                    |

## Ghi chú theo việc

Thêm mục `### <mã việc>` khi xong việc: đã làm khác thiết kế ở đâu, số đo, lệnh cần chạy, nợ kỹ thuật. Giữ ngắn.

### S01

- Kiên giao AI chốt thay nhóm theo hướng tối ưu: ADR-01, 03, 04, 07, 10, 11, 13, 14, 16, 17, 18 giữ nguyên như đề xuất. ADR-09 vẫn chờ số đo (D6); ADR-02 Kiên chốt riêng ngày 23/11/2026 (D5); ADR-15 là giả định, nhóm trưởng xin duyệt đề tài trước P1.
- **Đổi so với đề xuất:** ADR-08 luôn gửi Expo Push (nếu loại thông báo đang bật) kể cả khi người nhận đang nối WS; app đang mở thì tự ẩn banner. Lý do: Android giữ socket vài phút sau khi chuyển nền nên cách cũ làm mất thông báo. Đã sửa theo: `06-api.md` 6.3, sơ đồ trong `01-tong-the.md`. Ảnh hưởng P1-03 (server bớt tra `SimpUserRegistry`) và P1-04 (app thêm handler ẩn banner khi đang mở).
- BRD mục 9 sửa theo ADR-11.
- Cả nhóm nên đọc lại `11-adr.md` khi duyệt PR; ai phản đối ADR nào thì mở lại trong PR.

### S02

- Làm trước S01 theo yêu cầu của nhóm. Đã sửa: `docs/yeu-cau-nghiep-vu.md` (v0.2: FR-AUTH-01, 5.1, 5.2, 7.6, BR-04, NFR-09, mục 12, A1, R4, R11, Q3/Q5/Q6/Q7), `docs/ke-hoach.md`, `AGENTS.md` (bỏ cột phụ trách, thêm module `moderation`, `./mvnw verify`, thay "sheet" bằng file này), `docs/setup.md` (session pooler, Firebase Auth, `FIREBASE_PROJECT_ID`, health path).
- Vòng đời trạng thái mục 9 của BRD đã sửa theo ADR-11 trong S01.

### S03

- 22/11/2026: đã xóa hai biến `EXPO_PUBLIC_SUPABASE_*` khỏi `.env` máy Kiên. Các máy khác tự xóa rồi báo trong nhóm.

### S04

- 22/11/2026: project region `ap-south-1` (Mumbai, không phải Singapore; chậm hơn vài chục ms mỗi truy vấn, chấp nhận). Postgres 17.11, `vector` 0.8.2 và `postgis` 3.3.7 ở schema `extensions`, bucket `item-images` private, `max_connections` = 60.
- Server trên máy local nối qua session pooler thành công: `/api/v1/health?db=true` → `{"status":"up","db":"up"}`; Flyway đã tạo schema `refound` + `flyway_schema_history` trên Supabase.
- `anon` và `authenticated` không có quyền `USAGE` trên `refound` (lớp phòng thủ thứ hai của ADR-10 có sẵn, không cần `revoke`).
- Bí mật của server khi chạy local để trong `server/.env` (mẫu `server/.env.example`), xem `docs/setup.md`.
- Render nên chọn region Singapore (gần Mumbai nhất).

### S05

- 22/11/2026 (Kiên): bật Google trong Firebase Auth, khai SHA-1 (khóa debug local và khóa EAS), tải `google-services.json` mới, có Web client ID. Project EAS `@kiennd711/refound`, `projectId` điền tay trong `app.config.ts`.
- U18 mới đối chiếu tài liệu (xem `12-dieu-chua-chac.md`). Hai điểm P0-02 cần biết:
  - Tài liệu Firebase chỉ ghi URL khóa dạng X.509; URL dạng JWK mà ADR-05 dùng không có trong tài liệu nhưng chạy được và trả **cùng các khóa** (đã so `kid` ngày 22/11/2026). Giữ JWK vì `NimbusJwtDecoder.withJwkSetUri` đọc thẳng được.
  - Claim `firebase.sign_in_provider` **không** nằm trong danh sách claim bắt buộc của tài liệu. Chỉ bắt buộc claim này sau khi P0-03 giải mã token thật thấy nó; nếu thiếu thì bỏ kiểm tra (chỉ bật mỗi Google nên vẫn an toàn).

### S06

- 23/11/2026: cài bằng `npx expo install` mọi gói ở 8.1, thêm `@tanstack/react-query-persist-client` (cần cho persist cache). `npx expo install --check` báo khớp SDK 58; `npx expo config --type prebuild` nhận đủ plugin.
- 23/11/2026 (sau): thêm `expo-device`, `@react-native-community/netinfo`, `react-native-keyboard-controller` (rà P0–P2 thấy còn thiếu, tránh build lại). `react-native-view-shot` + `expo-sharing` cho FR-TAG-01 (P2) chưa cài.
- Plugin trong `app.config.ts`: MapLibre, secure-store, `@react-native-firebase/app` + `auth`, google-signin, `expo-location` / `expo-camera` / `expo-image-picker` (chuỗi xin quyền tiếng Việt, tắt micro).
- **Chưa cài `openapi-typescript`**: bản 7.13 đòi peer `typescript@^5`, repo dùng TypeScript 6. Khi cần sinh `src/types/api.d.ts` thì chạy `npx openapi-typescript@7 …` không cài vào repo.
- npm chặn script postinstall của `@firebase/util`, `protobufjs` (chính sách install-scripts mới của npm); chưa thấy ảnh hưởng, nếu build lỗi thì `npm install-scripts approve <gói>`.
- Còn lại (người làm): `npx expo run:android` (hoặc EAS development build) trên máy thật, mở được app; báo cả nhóm `git pull` + `npm ci` + build lại. Lúc đó biết U13, U17 có đúng không.

### S07

- Spring Boot **3.5.16** (bản 3.x cuối; start.spring.io giờ chỉ tạo 4.x nên `pom.xml` viết tay). Có sẵn web, security, websocket, validation, jdbc, flyway, postgresql để số đo U5 sát thực tế.
- `GET /api/v1/health` không chạm DB (dùng cho health check của Render); `?db=true` chạy `select 1`, DB lỗi → 503 `{"status":"up","db":"down"}`.
- `SecurityConfig` tối thiểu: chỉ mở `/health`, còn lại trả 403. P0-02 thay bằng cấu hình JWT.
- Số đo local (`docker run -m 512m --cpus=0.1`, Postgres 16 cạnh bên): sẵn sàng sau **77 s** (Spring báo 65 s), RSS **120 MiB**, 6 request `?db=true` đồng thời đều 200, không OOM. Trên 60 s nên khi đo Render nếu sát 90 s thì thử `spring.main.lazy-initialization=true` (7.5).
- Còn lại (người làm): tạo Web Service trên Render theo `docs/setup.md`, để ngủ 20 phút rồi đo `curl -w '%{time_total}'` 3 lần, ghi U4, U6, cập nhật U5 và D6.
- Lệnh: `cd server && ./mvnw spring-boot:run` cần Postgres ở `localhost:5432` hoặc `DATABASE_URL`; `docker build -t refound-server server`.

### S08

- 23/11/2026: Kiên bỏ việc này (D12). Không thử model trước; chụp bộ ảnh mẫu chuyển sang P0-13, ngưỡng `c0`, `c1` hiệu chỉnh lúc đó.

### S09

- `.github/workflows/ci.yml`: job `app` (Node 24, `npm ci`, `npm run check`) và `server` (Java 21, `./mvnw -B verify`).
- `.github/workflows/health-cron.yml`: 01:00 UTC mỗi ngày gọi `$SERVER_URL/api/v1/health?db=true`, chờ tối đa 120 s, thử 3 lần; chạy tay được (workflow_dispatch). Cần đặt biến repo `SERVER_URL` sau S07.
- Chưa chạy trên GitHub (chưa push). Đủ ✅ khi một PR hiện check xanh và cron chạy thành công một lần.

### S10

- `server/src/test/java/com/refound/support/TestDatabase.java`: container `supabase/postgres:17.12.0.002` cho test `*IT`, dùng bằng `@Import(TestDatabase.class)`. `TestDatabaseIT` kiểm Flyway chạy và có `vector`, `postgis` (U9 đúng). `./mvnw verify` qua: 4 unit + 1 IT.
- Test kết nối bằng `supabase_admin` (superuser), production dùng `postgres` qua pooler: migration cần quyền superuser thì sẽ qua test nhưng có thể lỗi trên Supabase, P0-01 phải chạy thử trên Supabase thật.
- Máy dùng engine không phải Docker Desktop (ví dụ Kiri, báo version 0.2.0): chạy `TESTCONTAINERS_RYUK_DISABLED=true TESTCONTAINERS_CHECKS_DISABLE=true ./mvnw verify`, và `DOCKER_HOST` trỏ đúng engine. CI Ubuntu không cần.
- `FakeFirebaseIdp` (23/11/2026): sinh khóa RSA, phục vụ JWKS ở `http://127.0.0.1:<cổng>/jwks`, ký ID token RS256 với claim giống Firebase; có `tokenSignedByOtherKey` cho ca sai khóa. Không thêm thư viện (JDK `HttpServer` + `Signature`). `FakeFirebaseIdpTest` kiểm chính nó. `./mvnw verify` qua: 5 unit + 1 IT.
- `TestUsers` cần `JwtIssuer` nên chuyển sang P0-02; chạy migration P0 trong test thuộc P0-01.

### P0-01

- `V202611230900__core_p0.sql` lấy nguyên SQL ở 02 §2.4, **sửa một lỗi**: `items_radius_ck` cũ cho qua tin `lost` có `radius_m = null` (`NULL OR false` = `NULL`, CHECK coi là đạt). Thêm `radius_m is not null`; 02 §2.4 đã sửa theo.
- Thêm 3 ràng buộc ngoài thiết kế (02 §2.4 đã sửa theo): `items_claimed_found_ck` (chỉ tin `found` được `claimed`, I3), `items_closed_reason_ck` (`closed` phải có `closed_reason`, I7/I12: admin gỡ tin cũng phải ghi lý do), unique `nulls not distinct (item_id, source, image_id)` trên `item_embeddings` (job chạy lại không sinh vector trùng; job phải xóa vector cũ trước khi ghi, như I2).
- `CoreP0MigrationIT`: 14 danh mục (3 nhạy cảm), chèn tin + vector 768 được, ràng buộc bán kính, 3 ràng buộc trên, vector sai số chiều bị từ chối. `./mvnw verify` qua: 5 unit + 14 IT.
- Cho P0-05/P0-06: lọc `bbox` của `GET /items` theo `public_location`, không theo `location` (lọc theo vị trí chính xác thì thu nhỏ khung dần là dò ra điểm thật, trái BR-11). Sửa tin sang danh mục nhạy cảm: xóa cả `ai_caption` (BR-08) và đặt `embedding_attempts = 0`.
- 23/11/2026: đã chạy trên Supabase thật bằng `./mvnw spring-boot:run` (user `postgres` qua session pooler, không cần superuser): Flyway áp `202611230900` thành công, `categories` 14 dòng (3 nhạy cảm), đủ CHECK mới. **Từ giờ không sửa file migration này**, cần đổi thì thêm migration mới.

## Nhật ký

| Ngày       | Ai           | Việc                                                                                                                                                 |
| ---------- | ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| 22/11/2026 | Nhóm         | Khởi tạo project Expo (commit `aea9c46`)                                                                                                             |
| 22/11/2026 | AI kiến trúc | Viết thiết kế kiến trúc v0.1 trong `docs/kien-truc/`                                                                                                 |
| 22/11/2026 | Nhóm         | Chốt D1 (đăng nhập Google / SĐT), D2 (signed URL), D3 (không chia việc cố định); tạo file tiến độ này                                                |
| 22/11/2026 | Nhóm         | Chốt D9 (commit thẳng file tiến độ khi nhận việc)                                                                                                    |
| 22/11/2026 | AI (Claude)  | S02: sửa BRD v0.2, đề cương, `AGENTS.md`, `docs/setup.md`; chốt D4 (cắt NFC, geofence)                                                               |
| 22/11/2026 | AI (Claude)  | S01: chốt các ADR còn đề xuất theo ủy quyền của Kiên; ADR-08 đổi sang luôn push; BRD mục 9 theo ADR-11                                               |
| 23/11/2026 | Kiên         | D5: chỉ dùng Gemini API (`gemini-embedding-2`), bỏ Groq, Jina; sửa ADR-02, 04 §4.6, BRD, `AGENTS.md`, `setup.md`                                     |
| 23/11/2026 | AI (Claude)  | Kiểm `server/.env`: đủ biến; Storage, Gemini (U3), Groq (U8) gọi thật thành công                                                                     |
| 23/11/2026 | AI (Claude)  | S06: thêm thư viện 8.1 + plugin; S10: `FakeFirebaseIdp` cho X23                                                                                      |
| 23/11/2026 | Kiên         | D11 (Groq `qwen/qwen3.8-27b` mô tả ảnh), D12 (bỏ S08), D13 (chấp nhận các giả định U), D6 (ADR-09 chốt); sửa ADR-02, 04, 07, 01, 02, BRD, `setup.md` |
| 22/11/2026 | Kiên         | S05: Firebase Auth (Google), SHA-1, `google-services.json`, project EAS; U18 đối chiếu tài liệu                                                      |
| 22/11/2026 | Kiên         | D10: bỏ đăng nhập SĐT + OTP, chỉ Google; sửa ADR-05, BRD v0.3 và tài liệu liên quan                                                                  |
| 22/11/2026 | Kiên + AI    | S04: Supabase xong, server local nối pooler thành công; ghi U1, U7                                                                                   |
| 23/11/2026 | AI (Claude)  | P0-01: migration P0 + `CoreP0MigrationIT` (sửa CHECK `radius_m`, thêm 3 ràng buộc); đã chạy trên Supabase                                            |
| 22/11/2026 | AI (Claude)  | S03 (máy Kiên), S07 phần code + đo local, S09 workflow, S10 phần Testcontainers (U9 đúng); chưa commit                                               |
