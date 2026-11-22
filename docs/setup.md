# Thiết lập lần đầu (việc V04)

Những bước này làm trên dashboard nên phải làm bằng tay. Chỉ TV1 làm, một lần.

## Supabase

Project dev đang dùng lại project của app cũ. Nhóm dùng **một project cho cả dev và demo**, không tạo project thứ hai (xem mục 5.4 kế hoạch).

- [x] Database: không còn bảng nào của app cũ trong schema `public` (kiểm tra ngày 19/11/2026).
- [x] Storage: xóa bucket `rewind-media` của app cũ (Dashboard → Storage → `rewind-media` → Delete bucket).
- [x] Lấy **anon / publishable key** (Project Settings → API) điền vào `.env` dưới tên `EXPO_PUBLIC_SUPABASE_ANON_KEY`, và `EXPO_PUBLIC_SUPABASE_URL`.
- [x] Xóa khỏi `.env` các biến của backend Spring cũ (`DATABASE_*`, `JWT_*`, `GROQ_*`, `JINA_*`, `SERVER_PORT`, `SPRING_PROFILES_ACTIVE`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_STORAGE_BUCKET`). App mới không dùng, và Expo nạp mọi biến trong `.env` khi chạy.
- [x] Đổi mật khẩu database và các key cũ (Groq, Jina) nếu từng bị lộ hoặc từng nằm trong commit cũ.
- [ ] Tắt xác nhận email (Authentication → Sign In / Providers → Email → tắt **Confirm email**). SMTP có sẵn của gói miễn phí chỉ gửi vài email mỗi giờ, không đủ cho ngày nhiều người đăng ký thử.
- [x] Mời 5 thành viên vào organization (Organization Settings → Team).
- [x] Liên kết repo với project dev: `npx supabase login` rồi `npx supabase link --project-ref <ref>`.

## Firebase (cho push notification, việc V30)

- [x] Tạo Firebase project, thêm app Android với package `com.viluoi.app` (khai trong `app.config.ts`).
- [x] Tải `google-services.json`, đưa lên EAS bằng biến môi trường dạng file thay vì commit.
- [x] Upload FCM service account key vào Expo (`npx eas-cli@latest credentials`).

## Gemini

- [x] Lấy API key ở Google AI Studio, điền vào `supabase/functions/.env` (copy từ `.env.example` cùng thư mục).
- [x] Không gửi key qua nhóm chat; mỗi người tự lấy key riêng cho máy mình nếu cần thử Edge Function.
