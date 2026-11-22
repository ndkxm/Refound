# Refound: đề cương

App tìm đồ thất lạc trong trường. Đồ án 3 tuần, 6 người, Android.

## Vấn đề

Mất đồ trong trường thường đăng lên nhóm Facebook: tin trôi nhanh, không đối chiếu được, ai cũng nhận vơ được.

## Phạm vi

**Lõi**

- Đăng nhập bằng Google hoặc số điện thoại + OTP, không có mật khẩu.
- Đăng đồ mất / nhặt được: ảnh, mô tả, vị trí trên bản đồ, thời gian.
- Tìm bằng vector search, xếp hạng theo ảnh/mô tả, khoảng cách, thời gian, danh mục.

**Giữa**

- Tự đối chiếu: khi có món mới, so với các tin đang mở và push khi vượt ngưỡng.
- Xác minh chủ nhân: người nhặt đặt câu hỏi kín; bàn giao quét QR hai bên; điểm uy tín.
- Chat ẩn danh realtime, không lộ số điện thoại.

**Mở rộng (cắt đầu tiên nếu trễ)**

- Bản đồ nhiệt điểm hay mất đồ.
- Nhãn QR dán lên đồ.

## Không làm

Train model riêng. Dùng model có sẵn; trọng số xếp hạng tự chỉnh trên bộ dữ liệu mẫu có đáp án.

Nhãn NFC và nhắc quên đồ bằng geofence: đã cắt ngày 22/11/2026 (cần thư viện native mới, vị trí nền không ổn định).

## Stack miễn phí

Backend Spring Boot 3 (Java 21, Maven) trong `server/`; Supabase làm Postgres (pgvector, PostGIS) và Storage, migration bằng Flyway; Groq (LLM và mô tả ảnh) và Jina `jina-clip-v2` (embedding ảnh và chữ), cả hai gói free, chỉ gọi từ server; server chạy trên Render gói free; Firebase Auth (Google, OTP qua SMS) cho đăng nhập, server tự phát hành JWT; Expo Push; MapLibre + OpenStreetMap; build app local bằng `npx expo run:android`, phát hành bằng APK. Chi tiết: `docs/yeu-cau-nghiep-vu.md` mục 12.

## Rủi ro

- Supabase free tạm dừng nếu không hoạt động một tuần: đặt lịch gọi nhẹ, kiểm tra trước demo.
- Hạn mức Groq free: lưu sẵn kết quả, chỉ gọi khi có món mới.
- Render free tự tắt khi không có truy cập, lần gọi đầu chậm: phải thiết kế trạng thái chờ và có phương án dự phòng khi demo.
- Embedding: Groq chưa thấy endpoint embeddings, nên mặc định dùng Jina; cần thử độ chính xác tiếng Việt và số token mỗi ảnh (Q3).
- SMS OTP của Firebase có thể không miễn phí: kiểm tra sớm; demo dùng số điện thoại thử.
- Ảnh có thẻ sinh viên / CCCD không gửi lên dịch vụ AI bên ngoài.

## Chưa quyết

Thầy đã duyệt đề tài chưa; model embedding (chờ thử nghiệm). Thiết kế: `docs/kien-truc/`; tiến độ và các quyết định: `docs/tien-do.md`. Nhóm không chia việc cố định theo người.
