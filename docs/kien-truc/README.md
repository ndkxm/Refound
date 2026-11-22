# Kiến trúc Refound

Tài liệu kiến trúc theo mục 15 của `docs/yeu-cau-nghiep-vu.md`. Phiên bản 0.2, 22/11/2026. Mọi mục ghi "Đề xuất" cần nhóm xác nhận. **Tiến độ và danh sách việc: [`docs/tien-do.md`](../tien-do.md).**

## Mục lục

| #   | File                                             | Nội dung                                                                                        |
| --- | ------------------------------------------------ | ----------------------------------------------------------------------------------------------- |
| 1   | [01-tong-the.md](01-tong-the.md)                 | Sơ đồ thành phần, luồng đăng tin / đối chiếu nền / bàn giao                                     |
| 2   | [02-du-lieu.md](02-du-lieu.md)                   | Bảng, cột, chỉ mục, migration mẫu P0                                                            |
| 3   | [03-phan-quyen.md](03-phan-quyen.md)             | Ma trận quyền, thực thi trong Spring, 23 test truy cập chéo                                     |
| 4   | [04-doi-chieu.md](04-doi-chieu.md)               | Embedding, công thức điểm + ví dụ tính tay, đối chiếu nền, chống trùng, đo top-5, thử nghiệm Q3 |
| 5   | [05-may-trang-thai.md](05-may-trang-thai.md)     | Bảng chuyển trạng thái Item và Claim                                                            |
| 6   | [06-api.md](06-api.md)                           | Endpoint REST, WebSocket, mã lỗi, OpenAPI                                                       |
| 7   | [07-chiu-loi-chi-phi.md](07-chiu-loi-chi-phi.md) | Hạn mức, thử lại, giới hạn tần suất, Render free, sao lưu                                       |
| 8   | [08-cau-truc-code.md](08-cau-truc-code.md)       | Thư mục app và server, hàm thuần cần test, thư viện cần thêm                                    |
| 9   | [09-kiem-thu.md](09-kiem-thu.md)                 | Tầng test, quy ước, kịch bản demo                                                               |
| 10  | [10-ke-hoach-3-tuan.md](10-ke-hoach-3-tuan.md)   | Nguyên tắc nhận việc, phụ thuộc, lịch, điểm cắt P2                                              |
| 11  | [11-adr.md](11-adr.md)                           | 18 ADR                                                                                          |
| 12  | [12-dieu-chua-chac.md](12-dieu-chua-chac.md)     | 18 giả định chưa kiểm chứng và cách kiểm tra                                                    |
| +   | [truy-vet.md](truy-vet.md)                       | Mã yêu cầu → thành phần → test                                                                  |
| +   | [van-de-phat-hien.md](van-de-phat-hien.md)       | 24 vấn đề trong yêu cầu và cấu hình, đề xuất xử lý                                              |

## Tóm tắt một trang

**Hình dạng hệ thống.** App Expo nói chuyện với **một** server Spring Boot trên Render free qua REST và một kênh WebSocket. Server giữ toàn bộ logic, khóa bí mật, lời gọi AI; nối Supabase Postgres (pgvector, PostGIS) qua session pooler và Storage bằng service key. Ảnh đọc bằng signed URL 1 giờ. Push qua Expo Push. Đăng nhập bằng Google hoặc số điện thoại + OTP qua Firebase Auth; server kiểm tra Firebase ID token rồi phát hành JWT riêng, không lưu email, SĐT hay mật khẩu.

**Ba phần khó, nằm trong code của nhóm, đều là hàm thuần có JUnit:**

1. **Đối chiếu** (`MatchScorer`): `S = 0,5·s_v + 0,2·s_d + 0,15·s_t + 0,15·s_c`, mỗi thành phần chuẩn hóa về [0, 1] (cosine cắt theo `c0, c1`; khoảng cách và thời gian giảm theo hàm mũ; danh mục 1 / 0,5 / 0), có lọc cứng và lý do khớp bằng tiếng Việt. Ngưỡng lưu 0,45, thông báo 0,65. Ví dụ tính tay sẵn cho test.
2. **Máy trạng thái** (`ItemStateMachine`, `ClaimStateMachine`): 14 chuyển trạng thái tin, 12 chuyển trạng thái claim; service ghi trong một transaction với `UPDATE ... WHERE status = :old`; bất biến quan trọng khóa thêm bằng partial unique index.
3. **Phân quyền** (`*Access`): Spring Security chỉ xác thực JWT + vai trò admin; quyền sở hữu kiểm tra ở service; DTO tách theo người xem; 23 test truy cập chéo.

**Đã chốt (22/11/2026).** Đăng nhập Google hoặc SĐT (ADR-05); ảnh qua signed URL (ADR-06); không chia việc cố định, nhận việc theo `docs/tien-do.md` (ADR-12).

**Đề xuất đáng chú ý.** Đối chiếu nền chạy trong tiến trình, hàng đợi là cột của `items` (không thêm dịch vụ). Vector **768 chiều** chốt ngay, hợp với cả Jina lẫn Gemini. Không tạo chỉ mục vector ở quy mô vài trăm tin. WebSocket chỉ là tín hiệu, mọi ghi qua REST. Bảng ở schema `refound`, không lộ qua Data API của Supabase.

**Giả định rủi ro nhất.**

1. Spring chạy ổn trong 512 MB / 0,1 CPU và thức dưới 90 giây (U5). Không có phương án Java miễn phí khác; đo ngày 1–2.
2. MapLibre RN và Firebase Auth native chạy với Expo SDK 58 / RN 0.88 (U13, U17).
3. Gửi SMS OTP tới số Việt Nam không tốn tiền ở gói miễn phí của Firebase (U16). Nếu tốn: chỉ dùng Google cho người thật, SĐT chỉ chạy với số thử khi demo.
4. Một ảnh tốn ~4.000 token Jina và số dư 10 triệu không hết hạn (U2).
5. `jina-clip-v2` đủ tốt với mô tả tiếng Việt (U10); đo bằng bộ mẫu.

**Còn chờ nhóm quyết** (bảng Quyết định trong `docs/tien-do.md`): duyệt các ADR còn "Đề xuất" (D8). Đã chốt thêm: cắt NFC và geofence (D4); BRD, đề cương, `AGENTS.md`, `docs/setup.md` đã sửa theo các quyết định (việc S02).
