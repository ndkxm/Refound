# Thiết lập lần đầu

Những bước này làm trên dashboard nên phải làm bằng tay. Mỗi phần do một người làm, một lần; nhận việc tương ứng trong `docs/tien-do.md` (S04 Supabase, S05 Firebase, S07 Render). Các ô chưa tích vì chưa làm.

## Supabase (chỉ làm Postgres và Storage)

Nhóm dùng **một project cho cả dev và demo**.

- [x] Tạo project Supabase mới (gói free).
- [x] Bật extension `vector` (pgvector) và `postgis` (Database → Extensions).
- [x] Tạo bucket Storage cho ảnh tin (để private, server cấp quyền truy cập).
- [x] Kiểm tra Project Settings → API (Data API): schema `refound` **không** nằm trong "Exposed schemas". Bảng của nhóm nằm ở schema này, do migration Flyway tạo (`docs/kien-truc/02-du-lieu.md`).
- [x] Lấy chuỗi kết nối **Session pooler** (nút Connect → Session pooler, dạng IPv4), không dùng "Direct connection" (có thể chỉ có IPv6, Render không nối được). Thêm `currentSchema=refound,extensions` vào chuỗi JDBC. Lấy khóa service role (Project Settings → API). Đặt tất cả vào **biến môi trường của server**. Không đưa vào app, không commit.
- [x] Ghi chú: gói free tạm dừng project sau 1 tuần không hoạt động (U1), cần kiểm tra trước buổi demo.
- Đã làm 22/11/2026 (S04): project ở region `ap-south-1` (Mumbai), Postgres 17.11, extension ở schema `extensions`, bucket `item-images` private.

### Chạy server trên máy mình với Supabase

1. Copy `server/.env.example` thành `server/.env` (đã bị `.gitignore` chặn), điền `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`. Mật khẩu lấy ở Project Settings → Database (quên thì "Reset database password"); không gửi qua nhóm chat.
2. `cd server && ./mvnw spring-boot:run`: Spring tự đọc `server/.env` (`spring.config.import` trong `application.yml`).
3. Mở `http://localhost:8080/api/v1/health?db=true`, thấy `"db":"up"` là được.

Trên Render không dùng file này, đặt biến ở mục Environment bên dưới.

## Render (chạy server Spring)

- [ ] Tạo Web Service từ repo, thư mục gốc `server/`, đóng gói bằng Docker. Chọn gói Free.
- [ ] Đặt biến môi trường (Environment):
  - `SPRING_PROFILES_ACTIVE=prod`
  - `DATABASE_URL` (chuỗi JDBC session pooler tới Supabase Postgres), `DATABASE_USERNAME`, `DATABASE_PASSWORD`
  - `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_STORAGE_BUCKET=item-images`
  - `JWT_SECRET` (chuỗi ngẫu nhiên ≥ 32 byte), `JWT_EXPIRATION_MS=604800000` (7 ngày)
  - `FIREBASE_PROJECT_ID` (không bí mật; dùng để kiểm tra `iss`, `aud` của Firebase ID token)
  - `GEMINI_API_KEY`, `GEMINI_EMBEDDING_MODEL=gemini-embedding-2`
  - `GROQ_API_KEY`, `GROQ_VISION_MODEL=qwen/qwen3.8-27b`
- [ ] Đặt health check path `/api/v1/health`.
- [ ] Hiểu giới hạn của gói Free: tự tắt sau khoảng 15 phút không có truy cập, khởi động lại chậm, RAM 512 MB. Điền địa chỉ Render vào `EXPO_PUBLIC_API_URL` của bản build thật.

## Gemini API (embedding)

- [ ] Tạo API key ở Google AI Studio (`aistudio.google.com`), gói free, không gắn thẻ. Đặt vào `GEMINI_API_KEY` của server (Render và `server/.env` của người chạy server).
- [ ] Hạn mức tính **theo project**, không theo key: cả nhóm nên dùng chung một project để biết tổng mức dùng; xem bảng hạn mức ở `aistudio.google.com/rate-limit`, ghi số của `gemini-embedding-2` vào U2.
- [ ] Gói free: nội dung gửi lên được Google dùng để cải thiện sản phẩm, nên không gửi ảnh danh mục nhạy cảm (đã có trong thiết kế, BR-08).
- [ ] Không gửi key qua nhóm chat.

## Groq (mô tả ảnh)

- [ ] Tạo API key ở `console.groq.com` (gói free), đặt vào `GROQ_API_KEY` của server (Render và `server/.env`).
- [ ] Model `qwen/qwen3.8-27b` đang ở dạng **preview** trên Groq (có thể bị gỡ khi báo trước ngắn): tên model để trong biến `GROQ_VISION_MODEL` để đổi không cần sửa code. Groq lỗi thì tin vẫn được embed, chỉ thiếu mô tả ảnh (ADR-02).
- [ ] Groq nằm sau Cloudflare: request không có header `User-Agent` hợp lệ (ví dụ mặc định của Python urllib) bị chặn `403 error code: 1010`. `GroqVisionClient` phải đặt `User-Agent` riêng.
- [ ] Xem hạn mức thật của org ở `console.groq.com/settings/limits` (tài liệu ngày 23/11/2026 ghi 30 request/phút, 1.000 request/ngày cho model này).

## Firebase (đăng nhập + push notification qua Expo)

- [ ] Tạo Firebase project (gói Spark, không gắn thẻ), thêm app Android với package `com.refound.app` (khai trong `app.config.ts`).
- [ ] Authentication → Sign-in method: bật **Google** (chọn email hỗ trợ), ghi lại **Web client ID** (S06 cần cho `google-signin`). Không bật Phone (D10).
- [ ] Project Settings → app Android: thêm **SHA-1 và SHA-256** của khóa ký debug (`cd android && ./gradlew signingReport` sau khi chạy `npx expo run:android`). Khi build APK phát hành thì thêm SHA của khóa release (việc H03). Thiếu SHA thì đăng nhập Google lỗi (`DEVELOPER_ERROR`).
- [ ] Demo cần hai tài khoản Google khác nhau cho hai máy; chuẩn bị trước.
- [ ] Tải `google-services.json` **sau khi** thêm SHA, đặt ở gốc repo khi build máy cá nhân; trên EAS dùng biến môi trường dạng file `GOOGLE_SERVICES_JSON`. Không commit file này.
- [ ] Upload FCM service account key vào Expo (`npx eas-cli@latest credentials`).

## Expo / EAS

- [x] Project EAS `@kiennd711/refound` đã tạo; `owner` và `extra.eas.projectId` điền tay trong `app.config.ts` (EAS không tự ghi được vào file `.ts`).
- [ ] Build development build: `npx eas-cli@latest build --profile development --platform android`, hoặc build local bằng `npx expo run:android`.

## Biến môi trường của app

Chỉ có `EXPO_PUBLIC_API_URL` (xem `.env.example`). Mọi bí mật khác nằm ở server.
