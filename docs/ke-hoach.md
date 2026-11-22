# Refound: đề cương

App tìm đồ thất lạc trong trường. Đồ án 3 tuần, 6 người, Android.

## Vấn đề

Mất đồ trong trường thường đăng lên nhóm Facebook: tin trôi nhanh, không đối chiếu được, ai cũng nhận vơ được.

## Phạm vi

**Lõi**
- Đăng đồ mất / nhặt được: ảnh, mô tả, vị trí trên bản đồ, thời gian.
- Tìm bằng vector search, xếp hạng theo ảnh/mô tả, khoảng cách, thời gian, danh mục.

**Giữa**
- Tự đối chiếu: khi có món mới, so với các tin đang mở và push khi vượt ngưỡng.
- Xác minh chủ nhân: người nhặt đặt câu hỏi kín; bàn giao quét QR hai bên; điểm uy tín.
- Chat ẩn danh realtime, không lộ số điện thoại.

**Mở rộng (cắt đầu tiên nếu trễ)**
- Nhãn QR / NFC dán lên đồ.
- Nhắc quên đồ bằng geofence.
- Bản đồ nhiệt điểm hay mất đồ; màn hình cho bảo vệ / văn phòng.

## Không làm

Train model riêng. Dùng model có sẵn; trọng số xếp hạng tự chỉnh trên bộ dữ liệu mẫu có đáp án.

## Stack miễn phí

Supabase (Postgres + pgvector + PostGIS, Auth, Storage, Realtime, Edge Functions), Gemini (gói free, chỉ gọi từ Edge Function), `gte-small` trong Edge Function cho embedding chữ, Expo Push/FCM, MapLibre + OpenStreetMap, build local bằng `npx expo run:android`, phát hành bằng APK.

## Rủi ro

- Supabase free tạm dừng nếu không hoạt động một tuần: đặt lịch gọi nhẹ, kiểm tra trước demo.
- Hạn mức Gemini free: lưu sẵn embedding, chỉ gọi khi có món mới.
- NFC và vị trí nền cần development build; NFC cần thư viện native thêm.
- Ảnh có thẻ sinh viên / CCCD không gửi lên Gemini free.

## Chưa quyết

Phân công 6 người, bảng dữ liệu, trọng số xếp hạng ban đầu, thầy đã duyệt đề tài chưa.
