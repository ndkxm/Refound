# Thiết lập lần đầu

Những bước này làm trên dashboard nên phải làm bằng tay. Mỗi phần do một người làm, một lần; nhận việc tương ứng trong `docs/tien-do.md` (S04 Supabase, S05 Firebase, S07 Render). Các ô chưa tích vì chưa làm.

## Supabase (chỉ làm Postgres và Storage)

Nhóm dùng **một project cho cả dev và demo**.

- [ ] Tạo project Supabase mới (gói free).
- [ ] Bật extension `vector` (pgvector) và `postgis` (Database → Extensions).
- [ ] Tạo bucket Storage cho ảnh tin (để private, server cấp quyền truy cập).
- [ ] Kiểm tra Project Settings → API (Data API): schema `refound` **không** nằm trong "Exposed schemas". Bảng của nhóm nằm ở schema này, do migration Flyway tạo (`docs/kien-truc/02-du-lieu.md`).
- [ ] Lấy chuỗi kết nối **Session pooler** (nút Connect → Session pooler, dạng IPv4), không dùng "Direct connection" (có thể chỉ có IPv6, Render không nối được). Thêm `currentSchema=refound,extensions` vào chuỗi JDBC. Lấy khóa service role (Project Settings → API). Đặt tất cả vào **biến môi trường của server**. Không đưa vào app, không commit.
- [ ] Ghi chú: gói free tạm dừng project nếu không có hoạt động trong một thời gian, cần kiểm tra trước buổi demo.

## Render (chạy server Spring)

- [ ] Tạo Web Service từ repo, thư mục gốc `server/`, đóng gói bằng Docker. Chọn gói Free.
- [ ] Đặt biến môi trường (Environment):
  - `SPRING_PROFILES_ACTIVE=prod`
  - `DATABASE_URL` (chuỗi JDBC session pooler tới Supabase Postgres), `DATABASE_USERNAME`, `DATABASE_PASSWORD`
  - `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_STORAGE_BUCKET=item-images`
  - `JWT_SECRET` (chuỗi ngẫu nhiên ≥ 32 byte)
  - `FIREBASE_PROJECT_ID` (không bí mật; dùng để kiểm tra `iss`, `aud` của Firebase ID token)
  - `GROQ_API_KEY`
  - `JINA_API_KEY`
- [ ] Đặt health check path `/api/v1/health`.
- [ ] Hiểu giới hạn của gói Free: tự tắt sau khoảng 15 phút không có truy cập, khởi động lại chậm, RAM 512 MB. Điền địa chỉ Render vào `EXPO_PUBLIC_API_URL` của bản build thật.

## Groq

- [ ] Lấy API key ở console của Groq, đặt vào `GROQ_API_KEY` của server (Render và máy local của người chạy server).
- [ ] Không gửi key qua nhóm chat; mỗi người tự lấy key riêng cho máy mình nếu cần chạy server.
- [ ] Chốt cách làm embedding (xem Q3 trong `docs/yeu-cau-nghiep-vu.md`) trước khi viết phần đối chiếu.

## Jina AI (embedding)

- [ ] Lấy API key ở trang Jina AI, đặt vào `JINA_API_KEY` của server. Kiểm tra số dư token và điều khoản trong bảng điều khiển (nhóm thấy khoảng 10 triệu token).
- [ ] Mỗi người tự lấy key riêng nếu cần chạy server trên máy mình; không gửi key qua nhóm chat.

## Firebase (đăng nhập + push notification qua Expo)

- [ ] Tạo Firebase project (gói Spark, không gắn thẻ), thêm app Android với package `com.refound.app` (khai trong `app.config.ts`).
- [ ] Authentication → Sign-in method: bật **Google** và **Phone**.
- [ ] Project Settings → app Android: thêm **SHA-1 và SHA-256** của khóa ký debug (`cd android && ./gradlew signingReport` sau khi chạy `npx expo run:android`). Khi build APK phát hành thì thêm SHA của khóa release (việc H03). Thiếu SHA thì đăng nhập Google và OTP đều lỗi.
- [ ] Authentication → Sign-in method → Phone → "Phone numbers for testing": thêm 2 số thử với mã OTP cố định, dùng khi phát triển và demo (không gửi SMS, không tốn tiền). Ghi số thử vào kênh nhóm, không commit.
- [ ] Kiểm tra gửi SMS OTP tới một số thật có miễn phí ở gói Spark không, ghi kết quả vào U16 trong `docs/kien-truc/12-dieu-chua-chac.md`. Không tự nâng gói Blaze khi chưa hỏi nhóm.
- [ ] Tải `google-services.json` **sau khi** thêm SHA, đặt ở gốc repo khi build máy cá nhân; trên EAS dùng biến môi trường dạng file `GOOGLE_SERVICES_JSON`. Không commit file này.
- [ ] Upload FCM service account key vào Expo (`npx eas-cli@latest credentials`).

## Expo / EAS

- [ ] Chạy `npx eas-cli@latest init` để tạo project EAS mới; lệnh sẽ ghi `projectId` vào `app.config.ts`.
- [ ] Build development build: `npx eas-cli@latest build --profile development --platform android`, hoặc build local bằng `npx expo run:android`.

## Biến môi trường của app

Chỉ có `EXPO_PUBLIC_API_URL` (xem `.env.example`). Mọi bí mật khác nằm ở server.
