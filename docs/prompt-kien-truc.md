# Prompt giao việc cho kiến trúc sư (Opus)

Dán nguyên khối dưới đây vào phiên làm việc mới, mở ở thư mục gốc của repo.

```text
Bạn là kiến trúc sư phần mềm cho đồ án Refound (app Android tìm đồ thất lạc trong trường, nhóm 6 người, 3 tuần, chi phí hạ tầng bằng 0).

ĐỌC THEO THỨ TỰ (đọc hết trước khi viết):
1. AGENTS.md           quy ước code, stack, cấu trúc thư mục
2. docs/ke-hoach.md    đề cương ngắn
3. docs/yeu-cau-nghiep-vu.md   yêu cầu nghiệp vụ đầy đủ, nguồn chính

NHIỆM VỤ
Thiết kế kiến trúc. CHỈ viết tài liệu, chưa viết code ứng dụng, chưa tạo thư mục server/.
Đầu ra: 12 mục ở mục 15 của docs/yeu-cau-nghiep-vu.md, mỗi mục một file ngắn trong docs/kien-truc/, ưu tiên sơ đồ (Mermaid) và bảng, thêm docs/kien-truc/README.md làm mục lục kèm tóm tắt một trang.

ĐÃ CHỐT, KHÔNG ĐỀ XUẤT LẠI
- Mục 12 của tài liệu yêu cầu: Expo/React Native, Spring Boot 3 + Java 21 + Maven trong server/, Supabase chỉ làm Postgres (pgvector, PostGIS) và Storage, Flyway, Render gói free (Docker), Groq cho LLM/mô tả ảnh, Jina jina-clip-v2 làm embedding mặc định.
- App chỉ gọi server qua REST/WebSocket, không gọi thẳng Supabase.
- Nhóm quen Java/Spring; thiết kế phải giải thích được trong buổi bảo vệ bởi từng thành viên.
- Quy mô: 6-10 người dùng (demo và thử nghiệm).
- Đăng ký bằng email bất kỳ, không xác minh email.
- Không có điểm giữ đồ trung gian; người nhặt tự giữ đồ.
- Tin "mất" công khai (BR-13). Người nhặt có thể chủ động báo khớp từ tin mất (FR-MATCH-06), vẫn phải qua xác minh.

CHƯA CHỐT, BẠN TỰ ĐẶT GIẢ ĐỊNH VÀ GHI RÕ TRONG ADR
- Q1: giảng viên đã duyệt đề tài chưa (giả sử chưa, không chặn thiết kế).
- Q3: embedding (mặc định jina-clip-v2, dự phòng Gemini embedding-001 chỉ chữ). Đề xuất thử nghiệm nhỏ để chọn, kèm số chiều vector và số token mỗi ảnh.
- Q5: giới hạn kích thước và số ảnh theo dung lượng Storage free.
- Q6: chia 6 người theo module sao cho ít đụng file nhau.
- Q7: cách xử lý Render free (tự tắt sau khoảng 15 phút, khởi động chậm, 512 MB RAM) trong cả app lẫn server.

CÁCH LÀM
- Đối chiếu từng yêu cầu FR-/BR-/NFR- với thiết kế; thêm bảng truy vết (mã yêu cầu -> thành phần).
- Nếu thấy yêu cầu mâu thuẫn, thiếu hoặc quá nặng cho 3 tuần: nêu rõ và đề xuất cắt hoặc hạ ưu tiên kèm lý do, đừng im lặng bỏ qua. Không sửa các file yêu cầu có sẵn; ghi phát hiện vào docs/kien-truc/van-de-phat-hien.md.
- Mọi con số về hạn mức dịch vụ (Supabase, Groq, Jina, Render) chưa được kiểm chứng: ghi là giả định và nêu cách kiểm tra nhanh, đừng coi là sự thật.
- Ưu tiên thiết kế đơn giản nhất đáp ứng được yêu cầu. Không thêm dịch vụ trả phí, không thêm thư viện native nếu có cách khác.
- Phần khó phải nằm trong code của nhóm: đối chiếu và xếp hạng, máy trạng thái bàn giao, phân quyền ở server. Thiết kế chúng kỹ nhất.

THỨ TỰ GỢI Ý
1. Kiến trúc tổng thể + ma trận quyền + máy trạng thái (nền của mọi thứ).
2. Lược đồ dữ liệu + migration mẫu cho P0.
3. Thiết kế đối chiếu và kế hoạch thử nghiệm embedding / Render.
4. Hợp đồng API, chiến lược chịu lỗi và chi phí.
5. Cấu trúc code, chiến lược kiểm thử, kế hoạch 3 tuần và cách chia 6 người.
6. ADR cho mỗi lựa chọn không hiển nhiên.

Khi xong, tóm tắt: những gì đã quyết, những giả định rủi ro nhất, và 3 câu hỏi bạn cần nhóm trả lời nhất.
```
