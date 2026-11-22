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

_Cập nhật: 22/11/2026_

- **Giai đoạn:** chuẩn bị (trước tuần 1). Chưa có code app ngoài khung Expo; chưa có thư mục `server/`.
- **Đã có:** khung Expo (commit `aea9c46`), tài liệu yêu cầu v0.2, thiết kế kiến trúc trong `docs/kien-truc/`; BRD, đề cương, `AGENTS.md`, `docs/setup.md` đã sửa theo các quyết định D1–D4 (S02). Tất cả **chưa gộp `main`**.
- **Đang chờ:** nhóm duyệt các ADR còn "Đề xuất" (S01); commit / PR cho thay đổi tài liệu của S02.
- **Làm được ngay, không phụ thuộc ai:** S01, S03, S04, S05, S08 (chụp ảnh bộ mẫu).
- **Rủi ro cần kiểm tra sớm nhất:** U5 (Spring trên Render 512 MB, việc S07), U13 + U17 (MapLibre, Firebase trên Expo SDK 58, việc S06), U16 (SMS OTP có miễn phí không, việc S05).

## Quyết định

| Mã  | Nội dung                                                                                                                                                        | Trạng thái               | Ngày       | Chi tiết                   |
| --- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------ | ---------- | -------------------------- |
| D1  | Đăng nhập bằng Google **hoặc** số điện thoại + OTP qua Firebase Auth; server kiểm tra Firebase ID token rồi phát hành JWT riêng; không lưu email, SĐT, mật khẩu | ✅ Chốt                  | 22/11/2026 | ADR-05                     |
| D2  | Ảnh đọc bằng signed URL 1 giờ do server cấp                                                                                                                     | ✅ Chốt                  | 22/11/2026 | ADR-06                     |
| D3  | Không chia việc cố định theo người; nhận việc theo file này                                                                                                     | ✅ Chốt                  | 22/11/2026 | ADR-12                     |
| D4  | Cắt NFC (FR-TAG-02) và geofence (FR-GEO-01)                                                                                                                     | ✅ Chốt                  | 22/11/2026 | van-de V19                 |
| D5  | Model embedding (Q3): `jina-clip-v2` hay phương án B                                                                                                            | ❓ Chờ kết quả S08       |            | ADR-02, 04-doi-chieu 4.6   |
| D6  | Server Spring chạy được trên Render free (Q7)                                                                                                                   | ❓ Chờ kết quả S07       |            | ADR-09                     |
| D7  | Bật SMS OTP thật hay chỉ dùng số thử khi demo                                                                                                                   | ❓ Chờ kết quả S05 (U16) |            | van-de V24                 |
| D9  | Được commit thẳng `docs/tien-do.md` lên `main` khi nhận việc hoặc báo bị chặn (chỉ file này)                                                                    | ✅ Chốt                  | 22/11/2026 | Mục "Cách dùng"            |
| D8  | Các ADR còn ghi "Đề xuất"                                                                                                                                       | ❓ Chờ S01               |            | `docs/kien-truc/11-adr.md` |

## Việc

### Chuẩn bị (tuần 1, ngày 1–2)

| Mã    | Việc                                                                                                                                        | Cần trước                             | File chính                                                  | Kết quả cần có                                                                                                                          | Thiết kế         | TT  | Người · nhánh / PR                           |
| ----- | ------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- | ----------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- | ---------------- | --- | -------------------------------------------- |
| S01   | Nhóm đọc thiết kế, quyết các mục ❓ (D4, D8)                                                                                                | —                                     | `docs/kien-truc/*`                                          | Mỗi ADR "Đề xuất" đổi thành "Chốt" hoặc ghi phương án thay thế; bảng Quyết định cập nhật                                                | README kiến trúc | ⬜  |                                              |
| S02   | Sửa BRD, `AGENTS.md`, `docs/setup.md` theo quyết định mới                                                                                   | S01                                   | `docs/yeu-cau-nghiep-vu.md`, `AGENTS.md`, `docs/setup.md`   | Hết các mâu thuẫn V1, V2, V9, V21, V23; `AGENTS.md` bỏ cột "Phụ trách" theo TV và thêm module `moderation`                              | van-de           | 🔵  | AI (Claude) · chưa commit; đổi ✅ khi PR gộp |
| S03   | Xóa `EXPO_PUBLIC_SUPABASE_*` khỏi `.env` mọi máy                                                                                            | —                                     | `.env` (local)                                              | Mỗi người xác nhận trong nhóm; `npm run check` không còn in hai biến này                                                                | van-de V22       | ⬜  |                                              |
| S04   | Thiết lập Supabase: project, extension `vector` + `postgis`, bucket private `item-images`, schema `refound` không nằm trong exposed schemas | —                                     | `docs/setup.md`                                             | Kết nối bằng chuỗi session pooler từ máy local thành công; ghi kết quả U1, U7                                                           | ADR-10, 12 U1 U7 | ⬜  |                                              |
| S05   | Thiết lập Firebase Auth: bật Google + Phone, khai SHA-1/SHA-256 khóa debug, tạo 2 số điện thoại thử                                         | —                                     | (console Firebase), `docs/setup.md`                         | Có số thử dùng được; ghi kết quả U16, U18                                                                                               | ADR-05           | ⬜  |                                              |
| S06 ◆ | Thêm thư viện một đợt (8.1) + build lại development build; bỏ `/supabase/` trong `jest.testPathIgnorePatterns`                              | S05                                   | `package.json`, `app.config.ts`                             | Development build mới trên máy thật: hiện bản đồ MapLibre, mở camera, đăng nhập Google thử được; `npm run check` qua; ghi U11, U13, U17 | 08 §8.1          | ⬜  |                                              |
| S07 ◆ | Dựng server Spring rỗng (Maven, Java 21, `GET /api/v1/health`), Dockerfile, deploy Render; đo cold start và RAM                             | S04                                   | `server/pom.xml`, `server/Dockerfile`, `server/.../common/` | URL Render trả `/api/v1/health?db=true` = up; số đo (thời gian thức, RSS, 6 request đồng thời) ghi vào Ghi chú; ghi U4, U5, U6          | ADR-09, 07 §7.5  | ⬜  |                                              |
| S08   | Chụp bộ ảnh mẫu (30 found, 30 lost, có "bẫy") + chạy thử nghiệm Q3 phương án A và B                                                         | — (chụp ảnh); S07 nếu chạy bằng JUnit | `server/src/test/resources/eval/`                           | `dataset.json` + ảnh; bảng top-5, MRR, token/ảnh, độ trễ của A và B; D5 được quyết; ghi U2, U3, U8, U10                                 | 04 §4.6, ADR-02  | ⬜  |                                              |
| S09   | CI GitHub Actions: `npm run check` + `./mvnw verify`; cron 1 lần/ngày gọi `/api/v1/health?db=true`                                          | S07                                   | `.github/workflows/`                                        | PR hiện check xanh/đỏ; cron chạy thành công một lần                                                                                     | 09 §9.2, 07 §7.5 | ⬜  |                                              |
| S10   | Nền test server: Testcontainers, fixture `TestUsers`, JWKS giả cho X23                                                                      | S07, P0-01                            | `server/src/test/java/.../support/`                         | Một test IT chạy migration P0 trên container và qua; ghi U9                                                                             | 09 §9.2          | ⬜  |                                              |

### P0 (tuần 1)

| Mã      | Việc                                                                                                                                                                                             | Cần trước           | File chính                                                                                             | Kết quả cần có                                                                                                                   | Thiết kế                | TT  | Người · nhánh / PR |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------- | ------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------- | --- | ------------------ |
| P0-01 ◆ | Migration P0                                                                                                                                                                                     | S04, S07            | `db/migration/V…__core_p0.sql`                                                                         | Flyway chạy được trên Supabase và Testcontainers; `categories` có 14 dòng                                                        | 02 §2.4                 | ⬜  |                    |
| P0-02 ◆ | Server đăng nhập: `POST /auth/firebase`, `JwtIssuer`, `SecurityConfig`, `UserStatusFilter`, `GET/PATCH /me`, `PUT /me/avatar`, `GET /users/{id}`, xử lý lỗi `ProblemDetail`                      | P0-01, S05          | `server/.../auth/`, `common/SecurityConfig`, `common/ApiExceptionHandler`                              | Test X17, X19, X23 xanh; token hết hạn → 401                                                                                     | ADR-05, 03, 06 §6.2     | ⬜  |                    |
| P0-03 ◆ | App đăng nhập: chọn Google / SĐT, nhập OTP, đặt tên lần đầu, `auth-store`, `api-client`, `query-client` (retry 7.5), banner chờ server thức                                                      | S06, P0-02          | `src/app/(auth)/*`, `src/features/auth/`, `src/lib/`, `src/stores/auth-store.ts`                       | Đăng nhập bằng Google và số thử trên máy thật; `normalize-phone.test.ts`, `is-retryable.test.ts` qua; banner hiện khi server ngủ | 08 §8.1, 07 §7.5        | ⬜  |                    |
| P0-04   | Design system tối thiểu: `Button`, `TextField`, `Screen` (đang tải / lỗi có nút thử lại / rỗng), `Card`; màu, cỡ chữ trong `tailwind.config.js`                                                  | S06                 | `src/components/ui/`, `tailwind.config.js`                                                             | Màn khác dùng được; vùng chạm ≥ 44dp; đủ tương phản sáng/tối                                                                     | AGENTS.md               | ⬜  |                    |
| P0-05 ◆ | Server tạo tin: `POST /items` multipart, `SupabaseStorageClient`, `LocationRounding`, giới hạn BR-09, validation                                                                                 | P0-01, P0-02        | `server/.../item/`                                                                                     | `LocationRoundingTest` dùng ví dụ 2.3; ảnh lên Storage thật; tin thứ 11 trong 24h → 429                                          | 02 §2.3, ADR-06, ADR-14 | ⬜  |                    |
| P0-06 ◆ | Server đọc/sửa tin: `GET /items` (bbox, lọc), `GET /items/{id}`, `/me/items`, `PATCH`, `close`, `mark-found`, `renew`, `DELETE`, `/items/{id}/secret`, ký signed URL theo lô, DTO theo người xem | P0-05, P0-09        | `server/.../item/`                                                                                     | Test X1–X6 xanh                                                                                                                  | 03, 05 §5.1, 06         | ⬜  |                    |
| P0-07   | App đăng tin: form found/lost, chụp/chọn ảnh, resize 1024/512, vị trí + bán kính, câu hỏi kín, lưu bản nháp                                                                                      | P0-03, P0-04, P0-05 | `src/features/items/`, `src/app/items/new.tsx`, `src/app/(tabs)/post.tsx`, `src/stores/draft-store.ts` | Đăng tin nhặt được dưới 1 phút trên máy thật; `item-form-schema.test.ts`, `resize-plan.test.ts` qua                              | 01 luồng 1, ADR-14      | ⬜  |                    |
| P0-08   | App bản đồ + danh sách + chi tiết + tin của tôi                                                                                                                                                  | P0-04, P0-06        | `src/app/(tabs)/index.tsx`, `src/app/items/[id]/`, `src/features/items/`                               | Đủ ba trạng thái; tin `found` hiện vị trí làm tròn; `format-distance.test.ts` qua                                                | FR-ITEM-03, 04          | ⬜  |                    |
| P0-09   | Hàm thuần nghiệp vụ: `ItemStateMachine`, `ClaimStateMachine`, `ItemAccess`, `ClaimAccess`, `AttemptPolicy`, `HandoverCode`                                                                       | S07                 | `server/.../{item,claim}/domain/`                                                                      | JUnit phủ mọi dòng bảng 5.1, 5.2, 5.3 và vài chuyển trạng thái sai                                                               | 05, 03 §3.3             | ⬜  |                    |
| P0-10 ◆ | Embedding: `EmbeddingClient` + Jina, `EmbeddingJob` (hàng đợi trên `items`, lùi dần, tạm dừng 5 phút khi 429/402), `embedding_cache`                                                             | P0-05, S08          | `server/.../matching/`                                                                                 | Tin mới → `embedding_status=done` trong dưới 1 phút khi server thức; giả lập Jina lỗi → `pending` và thử lại                     | 04 §4.1, 4.4, ADR-01    | ⬜  |                    |
| P0-11   | `MatchScorer` + `MatchReasons`                                                                                                                                                                   | S07                 | `server/.../matching/domain/`                                                                          | `MatchScorerTest` có 2 ví dụ tính tay ở 4.2 (0,776 và 0,320) + các ca biên                                                       | 04 §4.2                 | ⬜  |                    |
| P0-12 ◆ | Tìm kiếm: `POST /search` + màn tìm kiếm (chữ, ảnh)                                                                                                                                               | P0-08, P0-10, P0-11 | `server/.../matching/`, `src/app/(tabs)/search.tsx`, `src/features/matching/`                          | Kết quả có điểm + lý do; Jina lỗi → `degraded=true` và app báo; `score-label.test.ts` qua                                        | 04 §4.3                 | ⬜  |                    |
| P0-13   | Bộ đánh giá + lệnh seed                                                                                                                                                                          | S08, P0-10, P0-11   | `server/src/test/resources/eval/`, lệnh `--seed`                                                       | `./mvnw test -Dgroups=eval` in top-1, top-5, MRR; `--seed` nạp 60 tin kèm vector có sẵn                                          | 04 §4.6, 09 §9.3        | ⬜  |                    |

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
| P1-11   | Ảnh nhạy cảm (FR-ITEM-07, BR-08): không ký URL cho người không có quyền, job bỏ ảnh, tin `lost` nhạy cảm không có ảnh                    | P0-06, P0-10, P1-05 | `server/.../item/`, `server/.../matching/`                                                      | Test X5 xanh; job không gửi ảnh nhạy cảm cho Jina (test)                                   | 03 §3.2, 04 §4.1 | ⬜  |                    |
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

### S02

- Làm trước S01 theo yêu cầu của nhóm. Đã sửa: `docs/yeu-cau-nghiep-vu.md` (v0.2: FR-AUTH-01, 5.1, 5.2, 7.6, BR-04, NFR-09, mục 12, A1, R4, R11, Q3/Q5/Q6/Q7), `docs/ke-hoach.md`, `AGENTS.md` (bỏ cột phụ trách, thêm module `moderation`, `./mvnw verify`, thay "sheet" bằng file này), `docs/setup.md` (session pooler, Firebase Auth, `FIREBASE_PROJECT_ID`, health path).
- Chưa sửa: vòng đời trạng thái mục 9 của BRD (đề xuất ADR-11, chờ S01 duyệt).

## Nhật ký

| Ngày       | Ai           | Việc                                                                                                  |
| ---------- | ------------ | ----------------------------------------------------------------------------------------------------- |
| 22/11/2026 | Nhóm         | Khởi tạo project Expo (commit `aea9c46`)                                                              |
| 22/11/2026 | AI kiến trúc | Viết thiết kế kiến trúc v0.1 trong `docs/kien-truc/`                                                  |
| 22/11/2026 | Nhóm         | Chốt D1 (đăng nhập Google / SĐT), D2 (signed URL), D3 (không chia việc cố định); tạo file tiến độ này |
| 22/11/2026 | Nhóm         | Chốt D9 (commit thẳng file tiến độ khi nhận việc)                                                     |
| 22/11/2026 | AI (Claude)  | S02: sửa BRD v0.2, đề cương, `AGENTS.md`, `docs/setup.md`; chốt D4 (cắt NFC, geofence)                |
