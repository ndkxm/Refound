# Refound: Tài liệu yêu cầu nghiệp vụ (BRD)

| Mục                 | Nội dung                                                                                                                                                                                                                                                                     |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Phiên bản           | 0.2                                                                                                                                                                                                                                                                          |
| Ngày                | 22/11/2026                                                                                                                                                                                                                                                                   |
| Thay đổi so với 0.1 | Đăng nhập bằng Google hoặc số điện thoại + OTP thay cho email không xác minh (FR-AUTH-01); cắt NFC (FR-TAG-02) và geofence (FR-GEO-01); NFR-09 tách test app / server; Q6 đã quyết. Lý do: `docs/kien-truc/11-adr.md` (ADR-05, ADR-12), `docs/kien-truc/van-de-phat-hien.md` |
| Người đọc chính     | Kiến trúc sư (Opus), 6 thành viên nhóm, giảng viên                                                                                                                                                                                                                           |
| Bối cảnh            | Đồ án môn IE307, 3 tuần, 6 người, nền tảng chính là Android                                                                                                                                                                                                                  |
| Tài liệu liên quan  | `AGENTS.md` (quy ước code), `docs/ke-hoach.md` (đề cương ngắn)                                                                                                                                                                                                               |

> Cách đọc: mục 1–6 nói **cái gì và vì sao**. Mục 7–10 là yêu cầu có mã để truy vết. Mục 12 liệt kê những gì **đã chốt** (không đề xuất lại). Mục 15 nói rõ **đầu ra cần từ kiến trúc sư**. Những chỗ ghi _(đề xuất)_ là con số hoặc lựa chọn chưa được nhóm xác nhận.

---

## 1. Tóm tắt

Refound là app di động giúp người trong trường **tìm lại và hoàn trả đồ thất lạc**. Điểm khác với việc đăng tin lên nhóm Facebook:

1. **Tự đối chiếu**: tin "mất" và tin "nhặt được" được hệ thống so khớp liên tục (ảnh, mô tả, vị trí, thời gian) và báo khi có khả năng khớp.
2. **Xác minh chủ nhân**: người nhặt đặt câu hỏi kín, chỉ chủ thật mới trả lời đúng; bàn giao có bằng chứng (quét QR hai bên).
3. **Bảo vệ riêng tư**: liên lạc qua chat ẩn danh, không lộ số điện thoại; giấy tờ nhạy cảm không hiện ảnh công khai.

## 2. Bối cảnh và vấn đề

- Mất đồ trong trường (thẻ sinh viên, ví, chìa khóa, tai nghe, laptop, áo khoác, bình nước) rất thường xuyên.
- Cách hiện tại: đăng nhóm Facebook, hỏi bảo vệ, dán giấy. Tin trôi nhanh, không tìm kiếm được theo ảnh hay vị trí, người nhặt và người mất khó gặp nhau.
- Ai cũng nhận vơ được vì không có bước xác minh.
- Người nhặt ngại đăng vì sợ bị làm phiền hoặc lộ thông tin cá nhân.

**Yêu cầu đối với đề tài:** phần khó phải nằm trong **code của nhóm** (đối chiếu, xác minh, phân quyền, trạng thái), không chỉ là nhập liệu hay gọi một API AI.

## 3. Mục tiêu và thước đo thành công

| Mã  | Mục tiêu                                       | Thước đo _(đề xuất, cần nhóm xác nhận)_                                                         |
| --- | ---------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| G1  | Giúp người mất đồ thấy đúng món của mình nhanh | Trên bộ dữ liệu mẫu có đáp án, món đúng nằm trong top 5 kết quả ở ít nhất 70% truy vấn          |
| G2  | Giảm nhận vơ                                   | 100% bàn giao đi qua bước xác minh; không có đường tắt bỏ qua                                   |
| G3  | Chạy được hoàn toàn miễn phí                   | Chi phí hạ tầng bằng 0 đồng trong suốt đồ án                                                    |
| G4  | Demo trơn tru trên máy Android thật            | Kịch bản demo 5–7 phút chạy trên 2 máy mà không lỗi                                             |
| G5  | Có chiều sâu kỹ thuật để bảo vệ                | Giải thích được: công thức xếp hạng, máy trạng thái bàn giao, cách thực thi phân quyền ở server |

Không đặt mục tiêu về số lượng người dùng thật, vì đây là đồ án.

## 4. Người dùng

| Vai trò                    | Mô tả                                         | Nhu cầu chính                                                           |
| -------------------------- | --------------------------------------------- | ----------------------------------------------------------------------- |
| **Người mất đồ** (Owner)   | Sinh viên, giảng viên, nhân viên trong trường | Đăng tin mất, được báo khi có món giống, chứng minh là chủ, nhận lại đồ |
| **Người nhặt đồ** (Finder) | Bất kỳ ai nhặt được đồ                        | Đăng nhanh (dưới 1 phút), không lộ thông tin cá nhân, trả đồ đúng người |
| **Quản trị** (Admin)       | Thành viên nhóm / giảng viên                  | Xử lý báo cáo vi phạm, khóa tài khoản                                   |

Một người dùng có thể vừa là Owner vừa là Finder, ở các tin khác nhau. Vai trò Admin cần một thuộc tính phân quyền riêng. Không có vai trò điểm giữ đồ (xem 5.2).

## 5. Phạm vi

### 5.1 Trong phạm vi

- **P0 (lõi, bắt buộc xong tuần 1):** đăng ký / đăng nhập, đăng tin mất và tin nhặt được (ảnh, mô tả, danh mục, vị trí, thời gian), xem bản đồ và danh sách, tìm kiếm xếp hạng theo độ khớp.
- **P1 (giữa, tuần 2):** tự đối chiếu nền và thông báo đẩy, xác minh chủ nhân, bàn giao bằng QR, chat ẩn danh, điểm uy tín, báo cáo vi phạm.
- **P2 (mở rộng, tuần 3, cắt đầu tiên nếu trễ):** bản đồ nhiệt, nhãn QR dán lên đồ.

### 5.2 Ngoài phạm vi

- Train model riêng.
- Thanh toán, tiền thưởng cho người nhặt.
- Liên kết hệ thống quản lý sinh viên của trường (không có dữ liệu, không có quyền).
- Bản iOS, bản web.
- Điểm giữ đồ trung gian (bảo vệ, văn phòng khoa) và vai trò quản lý điểm giữ: người nhặt tự giữ đồ.
- Đăng ký bằng email + mật khẩu; giới hạn đăng ký theo email trường. (Đăng nhập chỉ qua Google hoặc số điện thoại, xem FR-AUTH-01.)
- Nhãn NFC (FR-TAG-02) và nhắc quên đồ bằng geofence (FR-GEO-01): **cắt ngày 22/11/2026** vì cần thư viện native mới ở tuần cuối và vị trí nền không ổn định giữa các hãng.
- Đa ngôn ngữ (chỉ tiếng Việt có dấu).
- Nhiều trường / nhiều tổ chức cùng lúc _(chỉ cần thiết kế để mở rộng được sau, không làm trong 3 tuần)_.

## 6. Quy trình nghiệp vụ chính

### 6.1 Người nhặt đăng tin

1. Chụp ảnh món đồ, chọn danh mục, ghi mô tả ngắn, xác nhận vị trí nhặt (mặc định lấy vị trí hiện tại, cho phép chỉnh trên bản đồ).
2. Đặt **một câu hỏi xác minh kín** (ví dụ "ốp lưng có hình gì?") và nhập đáp án đúng. Đáp án chỉ người nhặt xem được.
3. Người nhặt **tự giữ** món đồ và hẹn gặp chủ (không có điểm giữ đồ trung gian).
4. Hệ thống lưu tin, tạo biểu diễn tìm kiếm (embedding) và chạy đối chiếu với các tin "mất" đang mở.

### 6.2 Người mất tìm đồ

1. Đăng tin "mất" (cùng thông tin như trên, vị trí là khu vực nghi mất, bán kính tùy chọn), hoặc chỉ tìm kiếm.
2. Xem danh sách các tin "nhặt được" xếp theo độ khớp, kèm lý do khớp (ảnh giống, gần vị trí, cùng giờ).
3. Khi có tin mới đủ khớp, nhận thông báo đẩy dù app đang đóng.

### 6.3 Xác minh và bàn giao

1. Owner chọn một tin nhặt được và bấm "Đây là đồ của tôi".
2. Hệ thống hiển thị câu hỏi kín. Owner trả lời.
3. Finder xem câu trả lời và **quyết định** chấp nhận hoặc từ chối (hệ thống không tự so khớp chuỗi).
4. Nếu chấp nhận: mở kênh chat ẩn danh, hai bên hẹn giờ và nơi gặp.
5. Gặp mặt: Finder hiển thị mã QR bàn giao, Owner quét. Hệ thống ghi nhận bàn giao, đóng cả hai tin, cộng điểm uy tín cho Finder.
6. Nếu quá thời hạn không ai xử lý, tin hết hạn theo quy tắc BR-07.

## 7. Yêu cầu chức năng

Ưu tiên: **P0** bắt buộc, **P1** nên có, **P2** có nếu còn thời gian. "AC" là tiêu chí chấp nhận.

### 7.1 Tài khoản (AUTH)

| Mã         | Ưu  | Yêu cầu                                                                                                                                                     | AC                                                                                                                                                                                                                          |
| ---------- | --- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| FR-AUTH-01 | P0  | Đăng nhập bằng **Google** hoặc **số điện thoại + mã OTP qua SMS**, người dùng chọn một (qua Firebase Auth). Lần đầu đăng nhập là đăng ký. Không có mật khẩu | Đăng nhập xong vào được app; lần đầu nhập tên hiển thị; OTP sai hoặc hết hạn báo lỗi tiếng Việt rõ ràng; server không lưu email, số điện thoại, mật khẩu; khi demo dùng được số điện thoại thử của Firebase (không gửi SMS) |
| FR-AUTH-02 | P0  | Hồ sơ cơ bản: tên hiển thị, ảnh đại diện                                                                                                                    | Tên hiển thị là thứ duy nhất người khác thấy                                                                                                                                                                                |
| FR-AUTH-03 | P1  | Phân quyền: người dùng thường và quản trị                                                                                                                   | Quyền kiểm tra ở server, không chỉ ở giao diện                                                                                                                                                                              |
| FR-AUTH-04 | P1  | Đăng xuất, xóa tài khoản và dữ liệu cá nhân                                                                                                                 | Sau khi xóa, tin của người đó được ẩn danh hoặc xóa, chat bị xóa                                                                                                                                                            |

### 7.2 Đăng và quản lý tin (ITEM)

| Mã         | Ưu  | Yêu cầu                                                                  | AC                                                               |
| ---------- | --- | ------------------------------------------------------------------------ | ---------------------------------------------------------------- |
| FR-ITEM-01 | P0  | Đăng tin **nhặt được** với ảnh (1–4), danh mục, mô tả, vị trí, thời gian | Đăng xong trong dưới 1 phút trên mạng bình thường                |
| FR-ITEM-02 | P0  | Đăng tin **mất** với cùng thông tin, vị trí là khu vực kèm bán kính      | Có thể đăng không có ảnh                                         |
| FR-ITEM-03 | P0  | Danh sách và bản đồ các tin đang mở, lọc theo loại, danh mục, thời gian  | Đủ ba trạng thái: đang tải, lỗi (có nút thử lại), rỗng           |
| FR-ITEM-04 | P0  | Màn chi tiết tin                                                         | Không bao giờ hiển thị đáp án xác minh hay thông tin liên lạc    |
| FR-ITEM-05 | P0  | Sửa, đóng, xóa tin của chính mình                                        | Chỉ chủ tin mới thao tác được                                    |
| FR-ITEM-06 | P1  | Báo cáo tin vi phạm                                                      | Tin bị báo nhiều lần chuyển sang chờ duyệt                       |
| FR-ITEM-07 | P1  | Tin thuộc danh mục nhạy cảm (giấy tờ, thẻ) **ẩn ảnh công khai**          | Ảnh chỉ hiện sau khi xác minh xong; danh sách chỉ hiện mô tả chữ |

### 7.3 Tìm kiếm và đối chiếu (MATCH)

| Mã          | Ưu  | Yêu cầu                                                                                                                                               | AC                                                                                                     |
| ----------- | --- | ----------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| FR-MATCH-01 | P0  | Tìm kiếm bằng chữ và/hoặc ảnh, kết quả xếp theo **điểm khớp**                                                                                         | Mỗi kết quả có điểm và lý do khớp                                                                      |
| FR-MATCH-02 | P0  | Điểm khớp kết hợp: độ giống ảnh/mô tả, khoảng cách, chênh lệch thời gian, danh mục                                                                    | Công thức và trọng số nằm trong **một** module thuần, có unit test                                     |
| FR-MATCH-03 | P1  | Tự đối chiếu: mỗi khi có tin mới, so với các tin đối ứng đang mở và thông báo khi điểm vượt ngưỡng                                                    | Thông báo đến trong vòng _(đề xuất)_ 2 phút kể từ lúc đăng                                             |
| FR-MATCH-04 | P1  | Chống trùng: phát hiện cùng một món bị đăng hai lần                                                                                                   | Cảnh báo người đăng trước khi lưu                                                                      |
| FR-MATCH-05 | P0  | Bộ dữ liệu mẫu có đáp án và kịch bản đo độ chính xác                                                                                                  | Chạy được một lệnh, in ra tỉ lệ top-5                                                                  |
| FR-MATCH-06 | P1  | **Người nhặt chủ động báo khớp:** từ một tin "mất" công khai, người nhặt bấm "Tôi nhặt được món này", chọn hoặc tạo tin nhặt được của mình để gắn vào | Chủ tin mất nhận thông báo kèm tin đó và đi theo luồng xác minh (FR-CLAIM); không bỏ qua bước xác minh |

### 7.4 Xác minh và bàn giao (CLAIM)

| Mã          | Ưu  | Yêu cầu                                                              | AC                                                                              |
| ----------- | --- | -------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| FR-CLAIM-01 | P1  | Owner gửi yêu cầu nhận đồ, trả lời câu hỏi xác minh                  | Mỗi người chỉ gửi một yêu cầu đang mở cho mỗi tin                               |
| FR-CLAIM-02 | P1  | Finder chấp nhận hoặc từ chối yêu cầu                                | Từ chối cần một lý do ngắn; người bị từ chối có giới hạn số lần thử (BR-05)     |
| FR-CLAIM-03 | P1  | Chat ẩn danh realtime giữa hai bên, chỉ mở sau khi được chấp nhận    | Không lộ email, số điện thoại; tin nhắn đến trong dưới 2 giây khi cả hai online |
| FR-CLAIM-04 | P1  | Bàn giao bằng QR: Finder tạo mã, Owner quét                          | Mã chỉ dùng một lần, hết hạn sau _(đề xuất)_ 10 phút                            |
| FR-CLAIM-05 | P1  | Điểm uy tín: cộng khi bàn giao thành công, trừ khi bị báo cáo hợp lệ | Điểm hiển thị trên hồ sơ, ảnh hưởng đến giới hạn thử (BR-05)                    |
| FR-CLAIM-06 | P1  | Hủy bàn giao, khiếu nại                                              | Người dùng bấm "không phải chủ" hoặc "không đến" và có thể đánh dấu vi phạm     |

### 7.5 Thông báo (NOTI)

| Mã         | Ưu  | Yêu cầu                                                                                   | AC                                                    |
| ---------- | --- | ----------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| FR-NOTI-01 | P1  | Thông báo đẩy: có món khớp, có yêu cầu nhận, được chấp nhận, có tin nhắn mới, sắp hết hạn | Chạm vào thông báo mở đúng màn hình liên quan         |
| FR-NOTI-02 | P1  | Hộp thông báo trong app                                                                   | Đánh dấu đã đọc; không mất khi tắt thông báo hệ thống |
| FR-NOTI-03 | P1  | Người dùng bật/tắt từng loại thông báo                                                    | Cấu hình được lưu theo tài khoản                      |

### 7.6 Mở rộng (P2)

| Mã            | Yêu cầu                                                                                             | AC                                                              |
| ------------- | --------------------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| FR-HEAT-01    | Bản đồ nhiệt điểm hay mất đồ theo giờ trong ngày (làm trước)                                        | Dữ liệu tổng hợp, không lộ từng tin                             |
| FR-TAG-01     | Sinh nhãn QR cho một món đồ của mình để in dán; người nhặt quét là nhắn tới chủ, không cần đăng tin | Chủ nhận được thông báo kèm vị trí quét; không lộ danh tính chủ |
| ~~FR-TAG-02~~ | ~~Đọc nhãn NFC thay QR~~ **Đã cắt** (22/11/2026), xem 5.2                                           | —                                                               |
| ~~FR-GEO-01~~ | ~~Nhắc quên đồ bằng geofence~~ **Đã cắt** (22/11/2026), xem 5.2                                     | —                                                               |

## 8. Quy tắc nghiệp vụ

| Mã    | Quy tắc                                                                                                                                                                                                                   |
| ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| BR-01 | Một tin có đúng một loại: `lost` (mất) hoặc `found` (nhặt được). Chỉ đối chiếu giữa hai loại khác nhau.                                                                                                                   |
| BR-02 | Đáp án câu hỏi xác minh chỉ Finder đọc được. Không được trả về qua bất kỳ API hay truy vấn nào dành cho người khác.                                                                                                       |
| BR-03 | **Finder là người quyết định cuối cùng** việc chấp nhận một yêu cầu nhận. Hệ thống không tự động so khớp câu trả lời.                                                                                                     |
| BR-04 | Thông tin liên lạc (email, số điện thoại) không bao giờ lộ giữa người dùng. Mọi liên lạc qua chat trong app. Server không lưu email và số điện thoại; người dùng được định danh bằng mã tài khoản Firebase.               |
| BR-05 | Giới hạn thử nhận một tin: người điểm uy tín thấp bị giới hạn nhiều hơn _(đề xuất: tối đa 3 lần cho mỗi tin, mỗi người)_.                                                                                                 |
| BR-06 | Một tin chỉ đóng thành "đã trả" khi quét QR bàn giao thành công, hoặc khi Finder tự xác nhận kèm lý do.                                                                                                                   |
| BR-07 | Tin hết hạn sau _(đề xuất)_ 30 ngày không hoạt động; có thông báo trước 3 ngày; tin hết hạn không bị xóa mà chuyển trạng thái.                                                                                            |
| BR-08 | Tin thuộc danh mục nhạy cảm (thẻ sinh viên, CCCD, thẻ ngân hàng, hộ chiếu) không hiện ảnh công khai và không được gửi ảnh đó cho dịch vụ AI bên ngoài.                                                                    |
| BR-09 | Giới hạn tần suất đăng: _(đề xuất)_ tối đa 10 tin mỗi người mỗi ngày, để chống spam.                                                                                                                                      |
| BR-10 | Tin bị báo cáo hợp lệ ba lần chuyển sang chờ duyệt, ẩn khỏi danh sách công khai.                                                                                                                                          |
| BR-11 | Vị trí của tin "nhặt được" hiển thị ở mức chính xác tới tòa nhà _(đề xuất: làm tròn tọa độ)_ để tránh bị lợi dụng; vị trí chính xác chỉ lộ cho người đã được chấp nhận.                                                   |
| BR-12 | Mọi thời điểm lưu `timestamptz`, hiển thị theo múi giờ `Asia/Ho_Chi_Minh`.                                                                                                                                                |
| BR-13 | Tin "mất" **công khai** cho mọi người dùng đã đăng nhập, nên không được chứa thông tin liên lạc; vị trí hiển thị là khu vực kèm bán kính (không phải điểm chính xác); không có câu hỏi xác minh (chỉ tin "nhặt được" có). |
| BR-14 | Một bàn giao luôn phải đi qua xác minh của người nhặt (BR-03), kể cả khi người nhặt là người chủ động báo khớp (FR-MATCH-06).                                                                                             |

## 9. Vòng đời trạng thái

### 9.1 Tin (Item)

```
draft → open → matched → claimed → returned
          │        │         │
          │        │         └→ open        (yêu cầu bị từ chối / hủy)
          │        └→ open                 (khớp bị bỏ qua)
          ├→ expired                       (BR-07)
          ├→ under_review                  (BR-10)
          └→ closed                        (chủ tin tự đóng)
```

### 9.2 Yêu cầu nhận (Claim)

```
submitted → accepted → handover_pending → completed
    │           │              │
    │           │              └→ cancelled / disputed
    │           └→ cancelled
    └→ rejected
```

Kiến trúc sư cần chuyển thành bảng chuyển trạng thái đầy đủ (trạng thái hiện tại, hành động, ai được làm, điều kiện, trạng thái mới, tác dụng phụ). Các chuyển trạng thái chỉ xảy ra ở server, trong một transaction, **không** để app tự quyết định.

## 10. Mô hình dữ liệu mức khái niệm

Chỉ là thực thể và quan hệ, **chưa phải lược đồ**. Kiến trúc sư quyết định bảng, cột, chỉ mục.

| Thực thể           | Ý nghĩa                                             | Ghi chú truy cập                    |
| ------------------ | --------------------------------------------------- | ----------------------------------- |
| Profile            | Người dùng: tên hiển thị, ảnh, vai trò, điểm uy tín | Người khác chỉ thấy tên, ảnh, điểm  |
| Item               | Một tin mất hoặc nhặt được                          | Công khai trừ trường nhạy cảm       |
| ItemImage          | Ảnh của tin                                         | Ảnh danh mục nhạy cảm bị ẩn (BR-08) |
| ItemEmbedding      | Biểu diễn vector của tin để tìm kiếm                | Nội bộ, không trả về app            |
| VerificationSecret | Câu hỏi và đáp án kín của Finder                    | Đáp án chỉ Finder đọc (BR-02)       |
| MatchCandidate     | Cặp (tin mất, tin nhặt được) kèm điểm và lý do      | Chỉ hai chủ tin xem                 |
| Claim              | Yêu cầu nhận đồ và câu trả lời                      | Chỉ Owner và Finder của tin         |
| ChatMessage        | Tin nhắn trong một Claim                            | Chỉ hai bên                         |
| Handover           | Bản ghi bàn giao: mã QR, thời điểm, hai bên         | Chỉ hai bên và quản trị             |
| Notification       | Thông báo trong app                                 | Chỉ người nhận                      |
| PushToken          | Mã nhận thông báo đẩy của thiết bị                  | Chỉ chủ thiết bị                    |
| Report             | Báo cáo vi phạm                                     | Chỉ quản trị                        |
| ReputationEvent    | Lịch sử cộng/trừ điểm                               | Chỉ quản trị và chủ điểm            |
| Tag _(P2)_         | Nhãn QR gắn với một món đồ của người dùng           | Chủ nhãn                            |

Quan hệ chính: một Item có nhiều ItemImage, một VerificationSecret (chỉ tin `found`), nhiều MatchCandidate và nhiều Claim; một Claim có nhiều ChatMessage và tối đa một Handover.

Ma trận quyền chi tiết theo vai trò (Owner, Finder, Admin, người lạ) cho từng thực thể là **đầu ra bắt buộc** (mục 15).

## 11. Yêu cầu phi chức năng

| Mã     | Nhóm              | Yêu cầu                                                                                                                                                                                                                                                                  |
| ------ | ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| NFR-01 | Bảo mật dữ liệu   | Phân quyền thực thi ở server (Spring Security và kiểm tra ở tầng service). Mỗi endpoint có kiểm soát quyền phải có test truy cập chéo (người A gọi API lấy dữ liệu của người B phải bị từ chối), do người viết tự viết.                                                  |
| NFR-02 | Bí mật            | Khóa AI và khóa Supabase chỉ nằm trong biến môi trường của server. App chỉ biết URL của server, không gọi thẳng Supabase.                                                                                                                                                |
| NFR-03 | Riêng tư          | Tuân thủ BR-02, BR-04, BR-08, BR-11, BR-13. Ảnh thẻ không rời hệ thống của nhóm.                                                                                                                                                                                         |
| NFR-04 | Hiệu năng         | Mở danh sách dưới 2 giây trên mạng bình thường; tìm kiếm trả kết quả dưới 3 giây _(đề xuất)_.                                                                                                                                                                            |
| NFR-05 | Chịu lỗi          | Khi dịch vụ AI hết hạn mức hoặc lỗi, đăng tin vẫn thành công, việc tạo embedding chạy lại sau (hàng đợi thử lại).                                                                                                                                                        |
| NFR-06 | Chi phí           | Toàn bộ chạy trong gói miễn phí (mục 12). Phải thiết kế cache và giảm số lần gọi AI.                                                                                                                                                                                     |
| NFR-07 | Khả dụng cho demo | Có dữ liệu mẫu nạp được bằng một lệnh; có kịch bản demo không phụ thuộc mạng AI.                                                                                                                                                                                         |
| NFR-08 | Offline nhẹ       | Xem được danh sách và tin đã tải khi mất mạng; thao tác ghi báo lỗi rõ ràng, không mất nội dung đang soạn.                                                                                                                                                               |
| NFR-09 | Khả năng kiểm thử | Logic nghiệp vụ là hàm thuần có unit test. Ở server (JUnit, `./mvnw verify` phải qua): điểm khớp, máy trạng thái, phân quyền, làm tròn vị trí. Ở app (Jest, `npm run check` phải qua): định dạng khoảng cách/thời gian, chuẩn hóa số điện thoại, schema form.            |
| NFR-10 | Ngôn ngữ          | Mọi chữ hiển thị cho người dùng bằng tiếng Việt có dấu.                                                                                                                                                                                                                  |
| NFR-11 | Truy cập          | Vùng chạm tối thiểu 44dp; tương phản đủ trên chế độ tối và sáng.                                                                                                                                                                                                         |
| NFR-12 | Mở rộng sau này   | Thiết kế để có thể thêm "tổ chức" (nhiều trường) mà không viết lại lược đồ, dù chưa làm.                                                                                                                                                                                 |
| NFR-13 | Quy mô            | Dự kiến **6–10 người dùng** khi demo và thử nghiệm _(nhóm xác nhận ngày 22/11/2026)_. Giả định để tính dung lượng _(đề xuất)_: tối đa vài trăm tin, mỗi tin tối đa 4 ảnh. Không cần tối ưu cho tải cao, nhưng phải chịu được 6–10 người dùng đồng thời trên Render free. |

## 12. Ràng buộc và quyết định đã chốt

Những mục này **không đề xuất lại**. Nếu thấy mâu thuẫn với yêu cầu thì nêu rõ trong tài liệu kiến trúc.

| Chủ đề           | Quyết định                                                                                                                                                                                                                                                                                                                                                                                                              |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| App              | Expo SDK 58, React Native 0.88, React 19, TypeScript 6 strict, Expo Router (route nằm trong `src/app/`)                                                                                                                                                                                                                                                                                                                 |
| Giao diện        | NativeWind 4 (`className`), dùng lại `src/components/ui/`                                                                                                                                                                                                                                                                                                                                                               |
| Dữ liệu phía app | TanStack Query cho dữ liệu server, Zustand cho state cục bộ, react-hook-form + zod cho form                                                                                                                                                                                                                                                                                                                             |
| Backend          | **Spring Boot 3, Java 21, Maven**, đặt trong `server/`. Nhóm quen Java/Spring, đây là lý do chọn. Cấu trúc module theo `presentation / application / domain / infrastructure`                                                                                                                                                                                                                                           |
| CSDL và ảnh      | Supabase Postgres (pgvector, PostGIS) và Supabase Storage, gói miễn phí. Server nối qua JDBC; migration bằng Flyway                                                                                                                                                                                                                                                                                                     |
| AI               | **Groq** cho các tác vụ LLM và mô tả ảnh (chỉ gọi từ server). **Embedding: mặc định `jina-clip-v2` của Jina AI** (ảnh và chữ chung một không gian vector, gói free khoảng 10 triệu token theo OpenAPI của Jina, nhóm đã thấy số dư trong tài khoản). Groq chưa thấy endpoint embeddings trên trang chính thức (kiểm tra ngày 22/11/2026, chưa đọc hết trang). Dự phòng: Gemini `gemini-embedding-001` (chỉ chữ). Xem Q3 |
| Triển khai       | Server chạy trên **Render**, gói miễn phí, đóng gói Docker _(Render không có runtime Java riêng, cần xác nhận)_. App phát hành bằng APK                                                                                                                                                                                                                                                                                 |
| Thông báo        | Expo Push, server gọi bằng HTTP                                                                                                                                                                                                                                                                                                                                                                                         |
| Realtime         | WebSocket (STOMP) của Spring cho chat và cập nhật trạng thái                                                                                                                                                                                                                                                                                                                                                            |
| Đăng nhập        | Firebase Auth (Google hoặc số điện thoại + OTP) ở app; server kiểm tra Firebase ID token rồi tự phát hành JWT (Spring Security). Chốt 22/11/2026, ADR-05                                                                                                                                                                                                                                                                |
| Bản đồ           | MapLibre + OpenStreetMap (tránh cần thẻ thanh toán)                                                                                                                                                                                                                                                                                                                                                                     |
| Build            | Development build chạy local (`npx expo run:android`); phát hành bằng APK                                                                                                                                                                                                                                                                                                                                               |
| Chi phí          | **Bằng 0**. Chỉ dùng gói miễn phí                                                                                                                                                                                                                                                                                                                                                                                       |
| Quy mô           | 3 tuần, 6 người, Android                                                                                                                                                                                                                                                                                                                                                                                                |
| Thư viện mới     | Thư viện native mới phải hỏi người phụ trách khung trước, vì cả nhóm phải build lại                                                                                                                                                                                                                                                                                                                                     |

Quy ước code, thư mục `src/features/{items,matching,claims,auth,notifications}/` và quy tắc server đã có trong `AGENTS.md`. Kiến trúc sư đọc file đó trước.

## 13. Giả định và rủi ro

### Giả định

- A1: Hạn mức các gói miễn phí (Supabase, Groq, Jina, Render, Expo, Firebase Auth) đủ cho đồ án. Riêng SMS OTP của Firebase chưa rõ có miễn phí không (xem R11). **Chưa kiểm tra trang giá hiện tại**; riêng Jina nhóm đã thấy khoảng 10 triệu token trong tài khoản, nhưng chưa biết một ảnh tốn bao nhiêu token và gói có hết hạn không.
- A2: Người dùng có smartphone Android, cho phép quyền máy ảnh, vị trí và thông báo.
- A3: Nhóm có đủ ít nhất hai máy Android thật để demo.
- A4: Vector từ `jina-clip-v2` đủ tốt để xếp hạng, kể cả với mô tả tiếng Việt (chưa xác nhận tiếng Việt nằm trong các ngôn ngữ model hỗ trợ); chưa được kiểm chứng.

### Rủi ro

| Mã  | Rủi ro                                                                                                                                                                                                                               | Mức        | Giảm thiểu                                                                                                                                                                        |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| R1  | Dự án Supabase gói free bị tạm dừng khi không hoạt động                                                                                                                                                                              | Cao        | Lịch gọi nhẹ định kỳ; kiểm tra trước demo                                                                                                                                         |
| R9  | **Render gói free tự tắt sau khoảng 15 phút không có truy cập**, khởi động lại mất khoảng một phút hoặc hơn với Spring (cấu hình 512 MB RAM, 0.1 CPU theo trang của Render, chưa thử với app Java thật). WebSocket cũng bị ảnh hưởng | Cao        | App có trạng thái chờ và tự thử lại; đánh thức server trước buổi demo; không dùng cron ping liên tục vì Render phản đối cách này; tối ưu thời gian khởi động và bộ nhớ của Spring |
| R10 | Phân quyền nằm ở code nên dễ sót, không còn RLS làm lớp bảo vệ thứ hai                                                                                                                                                               | Trung bình | Test truy cập chéo bắt buộc cho từng endpoint (NFR-01); rà soát quyền theo ma trận                                                                                                |
| R2  | Hết hạn mức Groq hoặc Jina khi demo                                                                                                                                                                                                  | Cao        | Lưu sẵn embedding và kết quả mô tả, chỉ gọi khi có tin mới, dữ liệu mẫu đã xử lý sẵn                                                                                              |
| R3  | Chất lượng đối chiếu thấp (ảnh mờ, mô tả mơ hồ)                                                                                                                                                                                      | Trung bình | Bộ dữ liệu mẫu có đáp án; chỉnh trọng số; hiển thị lý do khớp để người dùng tự đánh giá                                                                                           |
| R4  | ~~Vị trí nền và NFC không ổn định trên một số hãng Android~~                                                                                                                                                                         | —          | Đã loại bỏ bằng cách cắt FR-TAG-02 và FR-GEO-01                                                                                                                                   |
| R11 | Gửi SMS OTP thật có thể tốn tiền hoặc cần gói Firebase gắn thẻ, trái G3                                                                                                                                                              | Trung bình | Kiểm tra ngày 1; nếu tốn tiền thì người dùng thật đăng nhập Google, số điện thoại chỉ dùng số thử khi demo                                                                        |
| R5  | Lạm dụng: đăng giả, nhận vơ, spam                                                                                                                                                                                                    | Trung bình | BR-03, BR-05, BR-09, BR-10, điểm uy tín                                                                                                                                           |
| R6  | Lộ thông tin cá nhân qua ảnh hoặc vị trí, nhất là vì tin "mất" công khai                                                                                                                                                             | Cao        | BR-02, BR-04, BR-08, BR-11, BR-13                                                                                                                                                 |
| R7  | Trễ tiến độ vì phạm vi lớn                                                                                                                                                                                                           | Cao        | Phân tầng P0/P1/P2; cắt từ P2                                                                                                                                                     |
| R8  | Đề tài chưa được giảng viên duyệt                                                                                                                                                                                                    | Trung bình | Xin duyệt trước khi bắt đầu P1                                                                                                                                                    |

## 14. Câu hỏi mở

| Mã  | Câu hỏi                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Ai quyết                            |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------- |
| Q1  | Giảng viên đã duyệt đề tài và phạm vi chưa?                                                                                                                                                                                                                                                                                                                                                                                                                  | Nhóm trưởng                         |
| Q3  | **Xác nhận lựa chọn embedding.** Mặc định `jina-clip-v2` (một model cho ảnh và chữ). Dự phòng: Groq mô tả ảnh thành chữ rồi embedding chữ bằng Gemini `gemini-embedding-001` (hoặc `jina-embeddings-v3`). Chạy cả hai trên bộ dữ liệu mẫu (FR-MATCH-05) và so tỉ lệ top-5, kèm đo token mỗi ảnh và độ trễ. Số chiều vector phải chốt trước khi viết migration. **Thiết kế:** số chiều chốt 768, thử nghiệm ở việc S08 (`docs/kien-truc/04-doi-chieu.md` 4.6) | Kiến trúc sư thử nghiệm, nhóm quyết |
| Q5  | Giới hạn kích thước và số ảnh để khớp dung lượng Storage miễn phí? **Đã trả lời:** tối đa 4 ảnh/tin, ảnh lớn 1024px ≤ 1 MB, ảnh nhỏ 512px (ADR-14)                                                                                                                                                                                                                                                                                                           | Kiến trúc sư                        |
| Q6  | ~~Phân công 6 người theo module nào?~~ **Đã quyết** (22/11/2026): không chia cố định, nhận việc theo `docs/tien-do.md`                                                                                                                                                                                                                                                                                                                                       | Nhóm                                |
| Q7  | Chiến lược cho Render free: cách xử lý lần gọi đầu chậm trong app và cách chuẩn bị trước buổi demo. Docker + Java có chạy ổn trong 512 MB RAM không? **Thiết kế:** ADR-09; đo ở việc S07                                                                                                                                                                                                                                                                     | Kiến trúc sư thử và báo kết quả     |

## 15. Đầu ra cần từ kiến trúc sư

Đặt trong `docs/kien-truc/`. Mỗi phần ngắn gọn, ưu tiên sơ đồ và bảng.

1. **Kiến trúc tổng thể**: sơ đồ thành phần (app, server Spring, Supabase Postgres/Storage, dịch vụ AI, push) và luồng dữ liệu cho ba kịch bản: đăng tin, đối chiếu nền, bàn giao.
2. **Lược đồ dữ liệu**: bảng, cột, kiểu, khóa, chỉ mục (kể cả vector và vị trí), kèm migration mẫu cho P0.
3. **Ma trận quyền và cách thực thi** theo vai trò × thực thể × thao tác, đặt ở lớp nào của Spring (filter, `@PreAuthorize`, service), và danh sách test truy cập chéo cần viết.
4. **Thiết kế đối chiếu**: quy trình tạo embedding, công thức điểm khớp và cách chuẩn hóa từng thành phần, nơi chạy đối chiếu nền (trigger, hàng đợi, lịch), cách chống trùng, cách đo top-5 trên bộ dữ liệu mẫu.
5. **Máy trạng thái** của Item và Claim ở dạng bảng chuyển trạng thái đầy đủ, và nơi thực thi.
6. **Hợp đồng API**: danh sách endpoint REST và kênh WebSocket, đầu vào, đầu ra, lỗi, quyền; đề xuất cách sinh tài liệu (OpenAPI).
7. **Chiến lược chịu lỗi và chi phí**: hàng đợi thử lại, cache, giới hạn tần suất, kế hoạch khi hết hạn mức, cách giữ dự án không bị tạm dừng.
8. **Cấu trúc code**: ánh xạ các yêu cầu vào `src/features/*`, ranh giới giữa `api/`, `hooks/`, `components/`, `utils/`, và các hàm thuần cần test.
9. **Chiến lược kiểm thử**: những gì có unit test, test phân quyền ở server, test tích hợp, kịch bản demo.
10. **Kế hoạch 3 tuần**: thứ tự xây dựng, phụ thuộc giữa các module, đề xuất chia 6 người sao cho ít đụng file nhau, điểm cắt rõ ràng của P2.
11. **Bản ghi quyết định (ADR)** ngắn cho mỗi lựa chọn không hiển nhiên (ví dụ Q3, nơi chạy đối chiếu nền, cách phát thông báo).
12. **Danh sách điều chưa chắc** mà thiết kế dựa vào (hạn mức, hành vi thư viện) và cách kiểm tra nhanh.

**Nguyên tắc cho kiến trúc sư:** ưu tiên thiết kế _đơn giản nhất đáp ứng được yêu cầu_, giải thích được trong buổi bảo vệ bởi từng thành viên. Không thêm dịch vụ trả phí, không thêm thư viện native nếu có cách khác. Nếu một yêu cầu quá nặng cho 3 tuần, đề xuất cắt hoặc hạ ưu tiên kèm lý do, đừng im lặng bỏ qua.

## 16. Thuật ngữ

| Thuật ngữ         | Nghĩa                                                                                |
| ----------------- | ------------------------------------------------------------------------------------ |
| Owner             | Người mất đồ                                                                         |
| Finder            | Người nhặt được đồ                                                                   |
| Item              | Một tin mất hoặc nhặt được                                                           |
| Claim             | Yêu cầu nhận một món nhặt được                                                       |
| Embedding         | Biểu diễn một đoạn chữ/ảnh thành vector để so độ giống                               |
| pgvector, PostGIS | Phần mở rộng của Postgres cho tìm kiếm vector và dữ liệu vị trí                      |
| Flyway            | Công cụ quản lý migration CSDL, mỗi thay đổi schema là một file đánh số              |
| Server            | Ứng dụng Spring Boot (Maven) trong `server/`, nơi giữ logic nghiệp vụ và khóa bí mật |
