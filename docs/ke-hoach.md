# Kế hoạch phát triển: **Ví Lười** — Quản lý tiền cho người lười nhập

> Đồ án môn IE307 — Lập trình đa nền tảng cho ứng dụng di động
> Nhóm: 6 thành viên · Nền tảng: **Android** (chính); iOS nếu nhóm có iPhone và tài khoản Apple Developer · React Native / Expo
> Tên app là tên tạm, nhóm có thể đổi.
>
> **Bản 2 (19/11/2026):** thu gọn MVP, chốt Android là nền tảng chính, bổ sung cách tính chi nhóm vào chi cá nhân, thêm chế độ thu nhập không đều cho safe-to-spend, cân lại phân công.
>
> **Bản 3 (19/11/2026):** rút lịch còn **3 tuần**, có dùng AI để viết code. Thu gọn phạm vi lần nữa (mục 4.7), viết lại lộ trình (mục 12), thêm quy tắc dùng AI (mục 13). Việc chi tiết từng ngày nằm ở sheet [Phân công Ví Lười 3 tuần](https://claude.ai/artifact/CpdnXJgfMP6nKvBSwB1bSK).

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Vấn đề và điểm khác biệt](#2-vấn-đề-và-điểm-khác-biệt)
3. [Đối tượng người dùng](#3-đối-tượng-người-dùng)
4. [Tính năng](#4-tính-năng)
5. [Công nghệ](#5-công-nghệ)
6. [Kiến trúc hệ thống](#6-kiến-trúc-hệ-thống)
7. [Thiết kế dữ liệu](#7-thiết-kế-dữ-liệu)
8. [Thuật toán và logic nghiệp vụ chính](#8-thuật-toán-và-logic-nghiệp-vụ-chính)
9. [Danh sách màn hình](#9-danh-sách-màn-hình)
10. [Cấu trúc thư mục](#10-cấu-trúc-thư-mục)
11. [Phân công 6 thành viên](#11-phân-công-6-thành-viên)
12. [Lộ trình phát triển](#12-lộ-trình-phát-triển)
13. [Quy trình làm việc nhóm](#13-quy-trình-làm-việc-nhóm)
14. [Kiểm thử](#14-kiểm-thử)
15. [Rủi ro và phương án dự phòng](#15-rủi-ro-và-phương-án-dự-phòng)
16. [Kịch bản demo](#16-kịch-bản-demo-khoảng-7-phút)
17. [Sản phẩm bàn giao](#17-sản-phẩm-bàn-giao)

---

## 1. Tổng quan

**Ví Lười** là app quản lý chi tiêu cá nhân và chi tiêu nhóm, xây dựng quanh một ý tưởng: **người dùng gần như không phải gõ tay**. Chi tiêu được ghi bằng giọng nói, bằng ảnh chụp màn hình chuyển khoản, hoặc tự động từ thông báo ngân hàng. Thay vì bắt người dùng đọc biểu đồ, app trả lời đúng một câu hỏi mỗi ngày: **"Hôm nay mình còn được tiêu bao nhiêu?"**

**Mục tiêu của đồ án:**

- Giải quyết một vấn đề có thật: phần lớn người dùng bỏ app ghi chi tiêu sau 1–2 tuần vì phải nhập tay.
- Khai thác tối đa khả năng riêng của điện thoại: micro, camera, thông báo hệ thống, GPS/geofence, push notification, widget, share sheet.
- Có phần realtime nhiều người dùng (quỹ nhóm, chia bill) để thể hiện năng lực làm backend.
- Khối lượng đủ lớn, chia rõ ràng cho 6 người.

---

## 2. Vấn đề và điểm khác biệt

| Vấn đề của app tài chính thông thường                             | Cách Ví Lười giải quyết                                                                                     |
| ----------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| Nhập tay từng khoản chi rất mất công, nên người dùng bỏ app       | Ghi bằng **giọng nói**, **ảnh chuyển khoản/hóa đơn** (OCR + AI), và **thông báo biến động số dư** (Android) |
| Biểu đồ đẹp nhưng không giúp quyết định nên tiêu hay không        | Một con số **"Còn được tiêu hôm nay"**, tự điều chỉnh theo ngày                                             |
| Chỉ nhắc khi đã tiêu lố, lúc đó đã muộn                           | **Cảnh báo theo địa điểm**: nhắc ngay khi bước vào nơi hay "cháy túi"                                       |
| Tiền chung với bạn bè, người yêu, phòng trọ ghi rời rạc trên Zalo | **Quỹ nhóm realtime**, chia bill, sổ nợ, **đòi nợ bằng VietQR** một chạm                                    |
| Các khoản trừ tự động hằng tháng bị quên                          | Tự **phát hiện khoản định kỳ** (Netflix, Spotify, gói 4G…) và nhắc trước ngày gia hạn                       |
| Báo cáo khô khan                                                  | **Trợ lý AI** trả lời câu hỏi bằng dữ liệu thật, quy đổi giá tiền ra số giờ làm việc                        |

### Định vị

Câu chuyện chính khi giới thiệu: **"Ghi chi tiêu không cần gõ, và hiểu cách người Việt nói về tiền."** Safe-to-spend là thứ người dùng nhận được nhờ dữ liệu luôn đầy đủ. Quỹ nhóm và bản đồ là phần mở rộng. Không nên giới thiệu app như một tập hợp 4 tính năng ngang hàng, vì từng tính năng riêng lẻ đều đã có app làm.

### 4 trụ cột (theo thứ tự ưu tiên)

1. **Zero-typing input**: nói, chụp, hoặc để app tự ghi. Hiểu tiếng lóng tiền tệ: _"1 xị"_, _"2 lít"_, _"1 củ"_, _"35 ca"_.
2. **Safe-to-spend**: con số "còn được tiêu hôm nay".
3. **Social money**: quỹ nhóm, chia bill, đòi nợ bằng VietQR.
4. **Location-aware**: bản đồ chi tiêu và cảnh báo theo địa điểm.

### Đối thủ và điểm khác biệt thật sự

| App                                   | Đã làm được                                    | Ví Lười khác ở đâu                                                                                                   |
| ------------------------------------- | ---------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Money Lover, Sổ Thu Chi MISA, Spendee | Ghi chép, ngân sách, báo cáo                   | Chủ yếu nhập tay                                                                                                     |
| MoMo                                  | Tự ghi giao dịch qua MoMo, quỹ nhóm, chia tiền | Chỉ trong hệ sinh thái MoMo. Ví Lười ghi được từ mọi ngân hàng (ảnh, thông báo) và đòi nợ bằng VietQR liên ngân hàng |
| Splitwise, Tricount                   | Chia bill, rút gọn nợ                          | Không có VietQR, không gắn với ngân sách cá nhân                                                                     |
| PocketGuard, Copilot                  | Con số "còn được tiêu"                         | Không hỗ trợ tiếng Việt, VNĐ, ngân hàng Việt Nam                                                                     |
| Rocket Money                          | Phát hiện khoản đăng ký định kỳ                | Không hoạt động ở Việt Nam                                                                                           |
| Cleo                                  | Chat AI về chi tiêu                            | Tiếng Anh, thị trường Mỹ/Anh                                                                                         |

> **Điểm thật sự mới:** (1) hiểu tiếng lóng tiền tệ tiếng Việt, có số liệu đo độ chính xác; (2) cảnh báo theo vị trí; (3) đòi nợ bằng VietQR; (4) đọc thông báo của mọi ngân hàng (Android). Các tính năng còn lại đã có app làm, giá trị nằm ở việc gộp chúng cho bối cảnh Việt Nam.
>
> Bảng trên được viết theo hiểu biết chung về thị trường. Nhóm cần tự cài và kiểm tra lại các app này trước khi viết phần khảo sát trong báo cáo.

---

## 3. Đối tượng người dùng

**Persona chính: sinh viên / người mới đi làm (18–25 tuổi)**

- Thu nhập không cố định: tiền gia đình gửi, làm thêm, thực tập. Vì vậy safe-to-spend có chế độ riêng cho thu nhập không đều ([mục 8.1](#81-safe-to-spend--còn-được-tiêu-hôm-nay)).
- Thanh toán chủ yếu bằng chuyển khoản / QR, nhận thông báo ngân hàng liên tục.
- Hay ăn uống, chia tiền với bạn bè, ở trọ ghép.
- Cuối tháng hay "hết tiền mà không biết tiêu vào đâu".

**Persona phụ:** cặp đôi hoặc nhóm bạn quản lý quỹ chung; phòng trọ chia tiền điện nước, đi chợ.

---

## 4. Tính năng

Ký hiệu độ ưu tiên:

- **P0**: bắt buộc cho MVP, thiếu là không demo được.
- **P1**: tính năng khác biệt, phải có trong bản nộp.
- **P2**: làm nếu còn thời gian.

### 4.1. Nền tảng

| ID   | Tính năng                                    | Mô tả                                                                                                                                                                 | Ưu tiên |
| ---- | -------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| F01  | Đăng ký / đăng nhập                          | Email + mật khẩu, Google Sign-In                                                                                                                                      | P0      |
| F02  | Onboarding thiết lập tài chính               | Chọn chế độ thu nhập (lương cố định / không đều), nhập thu nhập tháng, ngày nhận tiền (nếu lương cố định), các khoản cố định (trọ, điện nước, 4G), mục tiêu tiết kiệm | P0      |
| F03  | Ví / nguồn tiền                              | Tiền mặt, tài khoản ngân hàng, ví điện tử; số dư từng ví                                                                                                              | P0      |
| F04  | Danh mục                                     | Danh mục mặc định (Ăn uống, Di chuyển, Mua sắm…) + tạo danh mục riêng, icon, màu                                                                                      | P0      |
| F05  | Thêm / sửa / xóa giao dịch thủ công          | Form nhanh, bàn phím số, gợi ý danh mục                                                                                                                               | P0      |
| F06  | Lịch sử giao dịch                            | Lọc theo ngày, danh mục, ví; tìm kiếm                                                                                                                                 | P0      |
| F07  | Chống mất dữ liệu khi mạng yếu               | Thêm giao dịch hiện ngay trên UI; nếu mất mạng thì vào hàng đợi, tự gửi lại khi có mạng, chống trùng bằng `client_id` ([chi tiết](#luồng-khi-mạng-yếu-f07))           | P1      |
| F07b | Offline-first đầy đủ                         | Đọc / sửa / xóa khi không có mạng, đồng bộ hai chiều, xử lý xung đột                                                                                                  | P2      |
| F08  | Giao diện sáng / tối, đa ngôn ngữ Việt / Anh |                                                                                                                                                                       | P2      |
| F09  | Quyền riêng tư khi dùng AI                   | Màn đồng ý trước lần đầu gửi giọng nói / ảnh cho AI; giải thích dữ liệu đi đâu; không lưu ảnh gốc nếu người dùng không chọn lưu                                       | P1      |

### 4.2. Trụ cột 1 — Ghi chi tiêu không cần gõ ⭐

| ID  | Tính năng                                   | Mô tả                                                                                                                                                                         | Ưu tiên |
| --- | ------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| F10 | **Ghi bằng giọng nói**                      | Bấm giữ mic, nói _"trà sữa 35 nghìn"_, _"hôm qua đổ xăng 70k"_, _"lương về 5 triệu"_. AI tách số tiền, loại thu/chi, danh mục, ngày, ghi chú. Hiện màn xác nhận trước khi lưu | P1      |
| F11 | **Ghi bằng văn bản tự nhiên**               | Gõ nhanh 1 dòng giống F10 (cho lúc không tiện nói)                                                                                                                            | P1      |
| F12 | **Quét ảnh chuyển khoản / hóa đơn**         | Chụp hoặc chọn ảnh. AI đọc số tiền, người nhận, nội dung, thời gian. Hóa đơn nhiều món có thể tách từng dòng                                                                  | P1      |
| F13 | **Share vào app**                           | Từ app ngân hàng / Gallery, bấm _Chia sẻ → Ví Lười_, ảnh tự vào luồng F12                                                                                                     | P1      |
| F14 | **Tự ghi từ thông báo ngân hàng** (Android) | Đọc thông báo biến động số dư, tự tạo giao dịch "chờ xác nhận". Người dùng chỉ cần vuốt xác nhận                                                                              | P2      |
| F15 | **Học thói quen**                           | Nhớ "Highlands" → Ăn uống, "Grab" → Di chuyển; lần sau tự gán                                                                                                                 | P1      |
| F16 | Widget màn hình chính                       | Hiện số "còn được tiêu" + nút ghi nhanh                                                                                                                                       | P2      |

### 4.3. Trụ cột 2 — "Còn được tiêu hôm nay" ⭐

| ID  | Tính năng                | Mô tả                                                                                                                | Ưu tiên |
| --- | ------------------------ | -------------------------------------------------------------------------------------------------------------------- | ------- |
| F20 | **Con số Safe-to-spend** | Màn hình chính hiển thị to: _"Hôm nay còn 82.000đ"_. Công thức ở [mục 8.1](#81-safe-to-spend--còn-được-tiêu-hôm-nay) | P0      |
| F21 | Trạng thái màu           | Xanh (thoải mái) / vàng (sắp hết) / đỏ (vượt)                                                                        | P0      |
| F22 | Cuộn ngày                | Hôm nay tiêu ít thì phần dư chia đều cho các ngày còn lại; tiêu lố thì các ngày sau giảm                             | P1      |
| F23 | Ngân sách theo danh mục  | Giới hạn tháng cho từng danh mục, thanh tiến độ                                                                      | P1      |
| F24 | Mục tiêu tiết kiệm       | Ví dụ: "Mua laptop 15tr trước tháng 6", tính số tiền cần để dành mỗi ngày                                            | P1      |
| F25 | Báo cáo                  | Biểu đồ theo danh mục, theo ngày, so sánh tháng trước                                                                | P1      |

### 4.4. Trụ cột 3 — Chi tiêu theo địa điểm ⭐

| ID  | Tính năng           | Mô tả                                                                                                                                         | Ưu tiên |
| --- | ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| F30 | Gắn vị trí khi ghi  | Tự lấy GPS lúc ghi (có thể tắt)                                                                                                               | P1      |
| F31 | **Bản đồ chi tiêu** | Heatmap / marker những nơi tiêu tiền, bấm vào xem tổng chi tại đó                                                                             | P1      |
| F32 | **Vùng nguy hiểm**  | App gợi ý (nơi tiêu nhiều nhất) hoặc người dùng tự đặt vùng. Bước vào vùng mà danh mục liên quan đã dùng hơn 80% ngân sách thì nhận thông báo | P1      |
| F33 | Nhắc ghi chi tiêu   | Rời khỏi một quán sau hơn 15 phút: _"Bạn vừa ở Phúc Long, có chi gì không?"_                                                                  | P2      |

### 4.5. Trụ cột 4 — Tiền chung với bạn bè ⭐

| ID  | Tính năng                  | Mô tả                                                                                                                                                            | Ưu tiên |
| --- | -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| F40 | Tạo nhóm / quỹ             | Nhóm bạn, phòng trọ, cặp đôi, CLB. Mời bằng link hoặc mã QR                                                                                                      | P1      |
| F41 | **Chi tiêu nhóm realtime** | Một người thêm khoản chi, cả nhóm thấy ngay. Phần mỗi người phải chịu tự tính vào chi tiêu cá nhân của người đó ([chi tiết](#chi-tiêu-nhóm-và-chi-tiêu-cá-nhân)) | P1      |
| F42 | Chia bill                  | Chia đều / theo số tiền / theo phần trăm / theo món                                                                                                              | P1      |
| F43 | **Sổ nợ thông minh**       | Tự tính "ai nợ ai bao nhiêu", rút gọn số lần chuyển tiền (thuật toán ở [mục 8.3](#83-rút-gọn-nợ-trong-nhóm-f43))                                                 | P1      |
| F44 | **Đòi nợ bằng VietQR**     | Bấm "Nhắc", người nợ nhận push kèm mã VietQR đúng số tiền và nội dung                                                                                            | P1      |
| F45 | Xác nhận đã trả            | Người nhận xác nhận, nợ tự tất toán                                                                                                                              | P1      |
| F46 | Quỹ đóng góp               | Mỗi người đóng X/tháng, nhắc người chưa đóng                                                                                                                     | P2      |

### 4.6. Tính năng thông minh khác

| ID  | Tính năng                      | Mô tả                                                                                                              | Ưu tiên |
| --- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------ | ------- |
| F50 | **Tự phát hiện khoản định kỳ** | Tìm các khoản lặp lại cùng số tiền, chu kỳ khoảng 30 ngày. Gợi ý thêm vào danh sách định kỳ                        | P2      |
| F51 | Khoản định kỳ và nhắc gia hạn  | Người dùng tự thêm khoản định kỳ (lúc onboarding hoặc sau đó). Báo trước 2 ngày: _"Netflix sẽ trừ 260k vào thứ 5"_ | P1      |
| F52 | **Trợ lý AI**                  | Chat hỏi đáp: _"Tháng này mình tiêu gì nhiều nhất?"_, _"Nếu bớt 2 ly trà sữa/tuần thì cuối năm dư bao nhiêu?"_     | P2      |
| F53 | **Quy đổi giờ làm**            | Nhập lương theo giờ, mọi khoản chi hiện thêm _"= 3,5 giờ làm"_                                                     | P1      |
| F54 | Gamification                   | Streak "ngày không tiêu linh tinh", huy hiệu, thử thách tiết kiệm cùng bạn bè                                      | P2      |
| F55 | Xuất dữ liệu                   | Xuất CSV / Excel                                                                                                   | P2      |

### 4.7. Phạm vi MVP (bắt buộc hoàn thành)

Phạm vi cho lịch 3 tuần. Cột "Ưu tiên" trong các bảng ở trên là ưu tiên chung của sản phẩm; được làm hay không trong đồ án thì theo mục này.

| Nhóm                              | Tính năng                                                                                                                                                       |
| --------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Làm** (phải xong trước ngày 17) | F01 (chỉ email + mật khẩu), F02–F06, F09, F10, F11, F12, F15, F20–F23, F30, F31, F40–F45 (chia bill chỉ chia đều và theo số tiền), F51, F53                     |
| **Nếu kịp**                       | F07 hàng đợi gửi lại, F13 chia sẻ ảnh vào app, F24 mục tiêu tiết kiệm, F25 biểu đồ chi tiêu, F32 vùng nguy hiểm / geofence (chỉ làm nếu bản thử ngày 7 chạy ổn) |
| **Cắt**                           | Google Sign-In, F07b, F08, F14, F16, F33, F46, F50, F52, F54, F55, chia bill theo % và theo món                                                                 |

- MVP chạy trên **Android**. iOS là điểm cộng, không phải điều kiện hoàn thành.
- Kịch bản demo ([mục 16](#16-kịch-bản-demo-khoảng-7-phút)) chỉ dựa vào tính năng nhóm "Làm"; phần dùng tính năng "Nếu kịp" có phương án thay thế.
- Thứ tự cắt khi trễ nằm cuối [mục 12](#12-lộ-trình-phát-triển).

---

## 5. Công nghệ

### 5.1. Lựa chọn backend: **Supabase** (khuyến nghị)

| Tiêu chí                       | Supabase                                                                    | Firebase                                                 | Tự viết (Node/Spring) |
| ------------------------------ | --------------------------------------------------------------------------- | -------------------------------------------------------- | --------------------- |
| SDK cho React Native / Expo    | ✅ `@supabase/supabase-js` chạy tốt                                         | ✅ cần `@react-native-firebase` (dev build) hoặc web SDK | Tự viết API client    |
| Cơ sở dữ liệu                  | **PostgreSQL**: hợp với dữ liệu tài chính (quan hệ, SUM, GROUP BY, báo cáo) | Firestore NoSQL: báo cáo/tổng hợp khó hơn                | Tùy chọn              |
| Realtime (quỹ nhóm)            | ✅ Realtime có sẵn                                                          | ✅                                                       | Tự làm WebSocket      |
| Auth                           | ✅ Email, Google, OTP                                                       | ✅                                                       | Tự làm                |
| Lưu ảnh                        | ✅ Storage                                                                  | ✅                                                       | Tự làm                |
| Server function (gọi AI, cron) | ✅ Edge Functions (Deno/TS) + `pg_cron`                                     | ✅ Cloud Functions (cần gói trả phí Blaze)               | ✅                    |
| Phân quyền                     | **Row Level Security** ngay trong DB                                        | Security Rules                                           | Tự viết               |
| Gói miễn phí                   | Đủ cho đồ án                                                                | Đủ (trừ Functions)                                       | Cần tự host           |

**Kết luận:** dùng **Supabase**. SQL giúp phần báo cáo và sổ nợ dễ hơn nhiều, Edge Functions chạy miễn phí, và RLS là điểm kỹ thuật tốt để trình bày.

### 5.2. Stack chi tiết

| Lớp                     | Công nghệ                                                                  | Ghi chú                                                                                                                                                                 |
| ----------------------- | -------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Framework               | **Expo (SDK mới nhất)** + **TypeScript**                                   | Dùng **Development Build** (không dùng Expo Go) vì có native module                                                                                                     |
| Điều hướng              | **Expo Router**                                                            | File-based routing, tabs + stack + modal                                                                                                                                |
| UI                      | **NativeWind** (Tailwind cho RN)                                           | Nhóm đã quen từ bài tập trước                                                                                                                                           |
| Animation               | `react-native-reanimated`, `lottie-react-native`                           | Con số safe-to-spend, hiệu ứng ghi âm                                                                                                                                   |
| Biểu đồ                 | `victory-native` hoặc `react-native-gifted-charts`                         |                                                                                                                                                                         |
| State server            | **TanStack Query** + persist (AsyncStorage)                                | Cache, optimistic update; mutation bị tạm dừng khi mất mạng được lưu lại và tự gửi khi có mạng (F07)                                                                    |
| State client            | **Zustand**                                                                | Session, cài đặt                                                                                                                                                        |
| Form & validate         | `react-hook-form` + `zod`                                                  | Dùng chung schema zod cho kết quả AI                                                                                                                                    |
| Lưu cục bộ              | `@react-native-async-storage/async-storage`                                | Lưu cache và hàng đợi của TanStack Query. `expo-sqlite` chỉ cần khi làm F07b (P2)                                                                                       |
| Lưu bí mật              | `expo-secure-store`                                                        | Token đăng nhập                                                                                                                                                         |
| Ghi âm                  | `expo-audio`                                                               | Ghi file m4a rồi gửi lên Edge Function                                                                                                                                  |
| Camera / ảnh            | `expo-camera`, `expo-image-picker`, `expo-image-manipulator`               | Nén ảnh trước khi upload                                                                                                                                                |
| Share vào app           | `expo-share-intent`                                                        | Nhận ảnh từ app khác                                                                                                                                                    |
| Vị trí / geofence       | `expo-location` + `expo-task-manager`                                      | Geofencing chạy nền                                                                                                                                                     |
| Bản đồ                  | `react-native-maps`                                                        | Marker + heatmap                                                                                                                                                        |
| Thông báo               | `expo-notifications`                                                       | Local + push (Expo Push Service). Push trên Android cần cấu hình **FCM** (Firebase project); iOS cần APNs (tài khoản Apple Developer)                                   |
| Đọc thông báo ngân hàng | `react-native-android-notification-listener`                               | **Chỉ Android**, tính năng P2                                                                                                                                           |
| QR                      | `react-native-qrcode-svg`                                                  | Sinh VietQR (chuẩn EMVCo)                                                                                                                                               |
| Widget                  | `react-native-android-widget` (Android)                                    | P2                                                                                                                                                                      |
| AI                      | **Gemini API** (Flash) hoặc OpenAI, gọi qua **Edge Function**              | Key **không bao giờ** đặt trong app. Gói miễn phí có thể dùng dữ liệu gửi lên để huấn luyện mô hình; nếu có credit thì dùng gói trả phí, nếu không thì nói rõ trong F09 |
| Speech-to-text          | Gemini (nhận audio trực tiếp) hoặc OpenAI Whisper                          | Gộp luôn bước STT + phân tích trong một lần gọi                                                                                                                         |
| Backend                 | **Supabase**: Postgres, Auth, Realtime, Storage, Edge Functions, `pg_cron` |                                                                                                                                                                         |
| Build / phát hành       | **EAS Build**                                                              | Xuất file APK để demo và nộp                                                                                                                                            |
| Chất lượng code         | ESLint, Prettier, Husky + lint-staged                                      |                                                                                                                                                                         |
| Kiểm thử                | Jest + React Native Testing Library; Maestro (E2E, tùy chọn)               |                                                                                                                                                                         |

### 5.3. Nền tảng mục tiêu

- **Android là nền tảng chính.** Development build cho Android build được trên Windows (EAS), cài APK thẳng lên máy. Geofence và đọc thông báo ngân hàng đều kiểm thử trên Android.
- **iOS là điểm cộng.** Chạy development build trên iPhone thật gần như bắt buộc có tài khoản Apple Developer (99 USD/năm); không có thì chỉ chạy được simulator, mà simulator cần máy Mac. Chốt ở ngày 1 xem nhóm có điều kiện này không.

### 5.4. Project Supabase

- Có thể dùng lại project Supabase đã có, nhưng phải **xóa sạch schema, bảng và bucket của app cũ** rồi dựng lại hoàn toàn bằng `supabase/migrations/`. Không giữ bảng cũ "cho tiện".
- Dùng **một project cho cả dev và demo**. RLS tách dữ liệu theo người dùng, nên demo bằng một **tài khoản demo riêng** có dữ liệu seed đẹp thì không lẫn dữ liệu thử của nhóm.
- Vì dev và demo chung project: sau feature freeze (ngày 17) chỉ đẩy migration sửa lỗi đã được TV2 duyệt; từ ngày 20 không đẩy migration nào. Trước ngày bảo vệ lưu một bản sao bằng `supabase db dump`.
- Project gói miễn phí bị **tạm dừng sau khoảng 1 tuần không hoạt động**. Kiểm tra trước mỗi buổi demo và trước ngày bảo vệ.

---

## 6. Kiến trúc hệ thống

```
┌─────────────────────────── Mobile App (Expo) ───────────────────────────┐
│                                                                          │
│  UI (Expo Router + NativeWind)                                           │
│     │                                                                    │
│  Feature modules: auth · transactions · capture · budget · map · group   │
│     │                                                                    │
│  TanStack Query (cache + hàng đợi gửi lại)  ←→  Repository layer         │
│     │                                                                    │
│  Native: mic · camera · location/geofence · notifications · share intent │
└─────┼────────────────────────────────────────────────────────────────────┘
      │ HTTPS (supabase-js)            │ WebSocket (Realtime)
      ▼                                ▼
┌──────────────────────────────── Supabase ───────────────────────────────┐
│  Auth   │  PostgreSQL + RLS  │  Realtime  │  Storage (ảnh, audio)        │
│         │                    │            │                              │
│  Edge Functions:                                                         │
│   • parse-voice     audio → JSON giao dịch                               │
│   • parse-text      câu tự nhiên → JSON giao dịch                        │
│   • parse-image     ảnh → JSON giao dịch (1 hoặc nhiều dòng)             │
│   • ai-assistant    hỏi đáp trên dữ liệu người dùng                       │
│   • send-push       gửi push qua Expo Push API                           │
│  pg_cron:                                                                │
│   • detect-recurring   (mỗi đêm, P2)                                     │
│   • renewal-reminder   (mỗi sáng)                                        │
│   • daily-summary      (21:00 mỗi ngày)                                  │
└──────────────────────────────────┼───────────────────────────────────────┘
                                   ▼
                        Gemini / OpenAI API
```

### Luồng ghi bằng giọng nói (F10)

```
Bấm giữ mic → expo-audio ghi m4a → thả tay
  → upload lên Edge Function parse-voice (kèm danh sách danh mục của user)
  → Gemini: transcribe + trích xuất → JSON theo schema
  → app validate bằng zod → màn hình xác nhận (sửa được)
  → lưu vào bảng transactions (source = 'voice')
```

**Schema JSON AI phải trả về:**

```json
{
  "transactions": [
    {
      "type": "expense",
      "amount": 35000,
      "category": "Ăn uống",
      "note": "trà sữa",
      "occurred_at": "2026-11-19T15:30:00+07:00",
      "confidence": 0.92
    }
  ],
  "transcript": "trà sữa 35 nghìn"
}
```

> Dùng **structured output / JSON mode** của Gemini để kết quả luôn đúng schema.
> Nếu `confidence < 0.7`, highlight ô đó trên màn xác nhận để người dùng kiểm tra.

### Luồng khi mạng yếu (F07)

1. Giao dịch mới hiện ngay trên UI (optimistic update), kèm `client_id` (UUID sinh ở máy).
2. Nếu mất mạng, TanStack Query giữ mutation lại, lưu xuống AsyncStorage, và tự gửi khi `NetInfo` báo có mạng, kể cả sau khi tắt rồi mở lại app.
3. `client_id` là UNIQUE trên server nên gửi lại nhiều lần không tạo trùng (`insert ... on conflict (client_id) do nothing`).
4. Khi mất mạng, app chỉ hỗ trợ **thêm** giao dịch cá nhân. Sửa / xóa, chi tiêu nhóm và ghi bằng AI cần mạng. Giao dịch chưa gửi được hiện nhãn "đang chờ gửi".

> Offline-first đầy đủ (F07b, P2) mới cần `expo-sqlite`, đồng bộ hai chiều và xử lý xung đột.

### Chi tiêu nhóm và chi tiêu cá nhân

**Nguyên tắc:** safe-to-spend chỉ tính phần mình thực sự tiêu dùng. Tiền ứng hộ người khác hoặc tiền trả nợ làm thay đổi số dư ví, nhưng không phải là chi tiêu mới. Nếu không chốt nguyên tắc này từ đầu, app sẽ trừ tiền hai lần (lúc ăn và lúc trả nợ) hoặc trừ cả phần đã trả hộ.

**Loại giao dịch** (`transactions.type`):

| type       | Số dư ví                                  | Safe-to-spend          | Ví dụ                                 |
| ---------- | ----------------------------------------- | ---------------------- | ------------------------------------- |
| `income`   | Cộng                                      | Cộng vào tiền khả dụng | Lương, mẹ cho tiền                    |
| `expense`  | Trừ (không đổi nếu `wallet_id` null)      | Tính là chi            | Trà sữa; phần mình chịu trong bữa lẩu |
| `transfer` | Chuyển từ `wallet_id` sang `to_wallet_id` | Không tính             | Rút tiền ATM                          |
| `debt_out` | Trừ                                       | Không tính             | Ứng tiền hộ nhóm; trả nợ cho bạn      |
| `debt_in`  | Cộng                                      | Không tính             | Bạn trả nợ cho mình                   |

**Khi thêm một khoản chi nhóm**, hàm `fn_apply_group_expense` (gọi từ trigger) tự tạo giao dịch cá nhân, liên kết bằng `group_expense_id`:

- **Người trả tiền:** một `expense` bằng phần mình chịu và một `debt_out` bằng phần ứng hộ người khác, cùng trừ vào ví đã dùng để trả.
- **Các thành viên khác:** một `expense` bằng phần mình chịu, `wallet_id = null` vì chưa có tiền ra khỏi ví.
- Sửa hoặc xóa khoản chi nhóm thì xóa và tạo lại các giao dịch liên kết.

**Khi một khoản nợ được xác nhận đã trả** (`settlements.status = 'confirmed'`): tạo `debt_out` cho người trả và `debt_in` cho người nhận, liên kết bằng `settlement_id`. Mỗi người chọn ví lúc xác nhận, mặc định là ví ngân hàng chính.

**Ví dụ:** A trả lẩu 600k, chia đều 3 người.

|                                   | A                                       | B                         | C                         |
| --------------------------------- | --------------------------------------- | ------------------------- | ------------------------- |
| Lúc ăn                            | `expense` 200k + `debt_out` 400k (ví A) | `expense` 200k (không ví) | `expense` 200k (không ví) |
| Số dư ví thay đổi                 | −600k                                   | 0                         | 0                         |
| Safe-to-spend giảm                | 200k                                    | 200k                      | 200k                      |
| C trả A 200k                      | `debt_in` +200k                         |                           | `debt_out` −200k          |
| Safe-to-spend thay đổi khi trả nợ | 0                                       |                           | 0                         |

---

## 7. Thiết kế dữ liệu

### 7.1. Sơ đồ quan hệ (rút gọn)

```
profiles 1─n wallets
profiles 1─n categories
profiles 1─n transactions n─1 categories
                         n─1 wallets
profiles 1─n recurring_items
profiles 1─n budgets
profiles 1─n savings_goals
profiles 1─n danger_zones
groups   1─n group_members n─1 profiles
groups   1─n group_expenses 1─n expense_splits n─1 profiles
groups   1─n settlements
profiles 1─n merchant_rules
```

### 7.2. Các bảng chính

**profiles**

| Cột               | Kiểu                      | Ghi chú                                                     |
| ----------------- | ------------------------- | ----------------------------------------------------------- |
| id                | uuid PK                   | = `auth.users.id`                                           |
| display_name      | text                      |                                                             |
| avatar_url        | text                      |                                                             |
| income_mode       | enum `fixed` / `flexible` | Lương cố định / thu nhập không đều (mục 8.1)                |
| monthly_income    | bigint                    | VNĐ, lưu số nguyên                                          |
| payday            | smallint                  | Ngày nhận tiền (1–31), chỉ dùng khi `income_mode = 'fixed'` |
| hourly_wage       | bigint                    | Cho F53                                                     |
| bank_bin          | text                      | Mã ngân hàng, dùng cho VietQR                               |
| bank_account_no   | text                      | Số tài khoản nhận tiền                                      |
| bank_account_name | text                      |                                                             |
| expo_push_token   | text                      |                                                             |
| created_at        | timestamptz               |                                                             |

**transactions**

| Cột                    | Kiểu                                                                                | Ghi chú                                                                     |
| ---------------------- | ----------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| id                     | uuid PK                                                                             |                                                                             |
| client_id              | uuid UNIQUE                                                                         | Chống trùng khi đồng bộ offline                                             |
| user_id                | uuid FK                                                                             |                                                                             |
| wallet_id              | uuid FK                                                                             | Nullable: null khi là phần mình chịu trong khoản chi nhóm do người khác trả |
| to_wallet_id           | uuid FK                                                                             | Chỉ dùng cho `transfer`                                                     |
| category_id            | uuid FK                                                                             |                                                                             |
| type                   | enum `income` / `expense` / `transfer` / `debt_out` / `debt_in`                     | Xem [Chi tiêu nhóm và chi tiêu cá nhân](#chi-tiêu-nhóm-và-chi-tiêu-cá-nhân) |
| amount                 | bigint                                                                              | Luôn dương                                                                  |
| group_expense_id       | uuid FK                                                                             | Nullable: giao dịch tự sinh từ khoản chi nhóm                               |
| settlement_id          | uuid FK                                                                             | Nullable: giao dịch tự sinh khi trả nợ                                      |
| note                   | text                                                                                |                                                                             |
| merchant               | text                                                                                | Tên nơi chi, dùng để học thói quen                                          |
| occurred_at            | timestamptz                                                                         |                                                                             |
| source                 | enum `manual` / `voice` / `text` / `image` / `notification` / `recurring` / `group` | Thống kê nguồn nhập                                                         |
| lat, lng               | double precision                                                                    | Nullable                                                                    |
| place_name             | text                                                                                |                                                                             |
| attachment_url         | text                                                                                | Ảnh hóa đơn trong Storage                                                   |
| status                 | enum `confirmed` / `pending`                                                        | `pending` là giao dịch từ thông báo chưa xác nhận                           |
| created_at, updated_at | timestamptz                                                                         |                                                                             |

**Các bảng khác**

- **wallets**: id, user_id, name, type (`cash` / `bank` / `ewallet`), initial_balance, icon
- **categories**: id, user_id (null = mặc định), name, icon, color, type, is_essential
- **budgets**: id, user_id, category_id, month (date), limit_amount
- **recurring_items**: id, user_id, name, amount, category_id, cycle (`monthly` / `weekly` / `yearly`), next_due_date, is_fixed_cost, is_subscription, detected_by_system (bool), remind_before_days
- **savings_goals**: id, user_id, name, target_amount, saved_amount, deadline
- **merchant_rules**: id, user_id, keyword, category_id, hit_count _(F15 học thói quen)_
- **danger_zones**: id, user_id, name, lat, lng, radius_m, category_id, threshold_percent, enabled
- **groups**: id, name, type (`friends` / `roommates` / `couple` / `club`), invite_code, created_by
- **group_members**: group_id, user_id, role (`owner` / `member`), joined_at
- **group_expenses**: id, group_id, paid_by, amount, title, split_type (`equal` / `exact` / `percent` / `items`), occurred_at, receipt_url
- **expense_splits**: expense_id, user_id, share_amount
- **settlements**: id, group_id, from_user, to_user, amount, status (`requested` / `paid` / `confirmed`), created_at, confirmed_at

### 7.3. Bảo mật — Row Level Security

- Bảng cá nhân: `user_id = auth.uid()`.
- `profiles`: người khác **không** đọc được thu nhập, lương theo giờ. Thành viên cùng nhóm chỉ đọc được tên, avatar và thông tin nhận tiền (cho VietQR) qua view `v_group_member_public`.
- Bảng nhóm: chỉ thành viên mới đọc/ghi được.
  `exists (select 1 from group_members where group_id = ... and user_id = auth.uid())`
- Storage: mỗi user một thư mục `receipts/{user_id}/...`, policy tương ứng.
- API key AI chỉ nằm trong **secrets của Edge Function**.

### 7.4. View / function SQL hữu ích

- `v_monthly_spend_by_category`: tổng chi theo danh mục theo tháng.
- `fn_safe_to_spend(user_id, date)`: trả về con số safe-to-spend (mục 8.1).
- `fn_group_balances(group_id)`: số dư ròng từng thành viên.

---

## 8. Thuật toán và logic nghiệp vụ chính

### 8.1. Safe-to-spend — "Còn được tiêu hôm nay"

**Hai chế độ thu nhập** (`profiles.income_mode`):

- **Lương cố định (`fixed`)**: chu kỳ từ ngày nhận tiền (`payday`) đến trước ngày nhận tiền kỳ sau. Tháng không có ngày đó (ví dụ `payday = 31` vào tháng 2) thì dùng ngày cuối tháng.
- **Thu nhập không đều (`flexible`)**, mặc định cho sinh viên: chu kỳ là tháng dương lịch.

```
Tiền khả dụng của chu kỳ  = Nguồn tiền của chu kỳ
                          − Các khoản cố định (recurring_items.is_fixed_cost) của chu kỳ
                          − Mục tiêu tiết kiệm cần để dành trong chu kỳ

Nguồn tiền (fixed)        = max(Thu nhập thực nhận trong chu kỳ, monthly_income)
                            Khi thu nhập thực nhận còn thấp hơn monthly_income,
                            con số hiện nhãn "dự kiến" vì lương chưa về.

Nguồn tiền (flexible)     = Tổng số dư các ví lúc bắt đầu chu kỳ
                          + Thu nhập thực nhận trong chu kỳ

Còn lại đầu ngày hôm nay  = Tiền khả dụng − Chi biến đổi từ đầu chu kỳ đến hết hôm qua

Hạn mức hôm nay           = Còn lại đầu ngày hôm nay / Số ngày còn lại (tính cả hôm nay)

Còn được tiêu hôm nay     = Hạn mức hôm nay − Chi biến đổi hôm nay
```

- "Thu nhập" chỉ gồm giao dịch `income`. "Chi biến đổi" chỉ gồm giao dịch `expense`, **trừ** các khoản cố định đã tính ở trên (tránh trừ hai lần). `transfer`, `debt_out`, `debt_in` không được tính.
- Đổi `payday` hoặc đổi chế độ thu nhập giữa chu kỳ thì áp dụng từ chu kỳ sau.
- Chế độ `fixed`: tiền dư cuối chu kỳ không tự cộng sang chu kỳ sau, app gợi ý chuyển vào mục tiêu tiết kiệm. Chế độ `flexible` tự mang sang vì tính từ số dư ví.
- Màu trạng thái: ≥ 50% hạn mức là xanh, 0–50% là vàng, < 0 là đỏ.
- **Ví dụ:** khả dụng 3.000.000đ, đến hôm qua đã tiêu 1.800.000đ, còn 12 ngày thì hạn mức = 100.000đ/ngày. Hôm nay tiêu 35.000đ thì **còn được tiêu 65.000đ**.

### 8.2. Phát hiện khoản định kỳ (F50)

Chạy mỗi đêm bằng `pg_cron` / Edge Function:

1. Lấy giao dịch chi 120 ngày gần nhất của mỗi user.
2. Gom nhóm theo `merchant` (đã chuẩn hóa: viết thường, bỏ dấu, bỏ số) **và** số tiền (sai lệch ≤ 5%).
3. Nhóm có ≥ 2 lần xuất hiện, khoảng cách giữa các lần là 28–32 ngày (hoặc 6–8 ngày cho chu kỳ tuần) thì là ứng viên.
4. Tạo `recurring_items` với `detected_by_system = true`, gửi thông báo hỏi người dùng xác nhận.

### 8.3. Rút gọn nợ trong nhóm (F43)

1. Tính **số dư ròng** mỗi thành viên = tổng đã trả hộ − tổng phần phải chịu − đã nhận thanh toán + đã thanh toán.
2. Chia thành hai danh sách: **chủ nợ** (dư > 0) và **con nợ** (dư < 0).
3. Thuật toán tham lam: ghép con nợ âm nhiều nhất với chủ nợ dương nhiều nhất, chuyển `min(|nợ|, có)`, cập nhật, lặp đến khi hết.
4. Kết quả: tối đa _n − 1_ lần chuyển khoản thay vì chuyển chéo qua lại.

> Ví dụ: A trả 600k cho 3 người (A, B, C), B trả 300k cho 3 người.
> Mỗi người chịu 300k. Số dư ròng: A = +300k, B = 0, C = −300k
> → chỉ cần **C chuyển cho A 300k**.

### 8.4. Sinh mã VietQR (F44)

- Tạo chuỗi theo chuẩn **EMVCo / NAPAS VietQR** ngay trên máy, gồm: BIN ngân hàng, số tài khoản, số tiền, nội dung (ví dụ `VILUOI NHOM ABC TRA NO`), CRC16.
- Render bằng `react-native-qrcode-svg`. Không phụ thuộc dịch vụ bên ngoài.
- Phương án đơn giản hơn: dùng ảnh từ dịch vụ Quick Link của VietQR (`img.vietqr.io`).

### 8.5. Geofence cảnh báo (F32)

1. Khi bật tính năng, đăng ký các vùng trong `danger_zones` bằng `Location.startGeofencingAsync` (iOS giới hạn khoảng 20 vùng, cần chọn vùng quan trọng nhất).
2. Task nền `expo-task-manager` nhận sự kiện `Enter`.
3. Đọc ngân sách danh mục từ cache cục bộ. Nếu đã dùng ≥ `threshold_percent` thì bắn **local notification**:
   _"Bạn vừa đến Vincom. Tháng này Mua sắm đã dùng 85% ngân sách."_
4. **Gợi ý vùng tự động:** gom cụm các giao dịch có tọa độ (khoảng cách < 150m), lấy top 5 cụm tổng chi cao nhất.

**Lưu ý khi chạy thật:** Android 10+ phải xin riêng quyền vị trí nền ("Luôn cho phép"). Một số ROM (Xiaomi, Oppo, Vivo) tự tắt tác vụ nền, cần màn hướng dẫn tắt tối ưu pin cho app. Khi demo, dùng vị trí giả lập (mock location) trên Android.

### 8.6. Học thói quen danh mục (F15)

- Khi người dùng lưu giao dịch có `merchant`, tăng `hit_count` cho cặp (keyword → category) trong `merchant_rules`.
- Lần sau: khớp keyword trước (nhanh, offline). Không khớp mới gọi AI.
- Danh sách rule cũng được gửi kèm trong prompt AI để AI phân loại đúng thói quen người dùng.

---

## 9. Danh sách màn hình

**Tab chính (Bottom Tabs):**

| Tab        | Màn hình        | Nội dung                                                                                           |
| ---------- | --------------- | -------------------------------------------------------------------------------------------------- |
| 🏠 Hôm nay | Home            | Con số safe-to-spend lớn, vòng tiến độ, giao dịch hôm nay, giao dịch chờ xác nhận, nút **mic** nổi |
| 📒 Sổ      | Transactions    | Lịch sử, lọc, tìm kiếm; chi tiết / sửa giao dịch                                                   |
| ➕ Ghi     | Capture (modal) | 4 cách: **Nói** · **Gõ câu** · **Chụp/chọn ảnh** · **Nhập tay**                                    |
| 🗺️ Bản đồ  | Spending Map    | Bản đồ chi tiêu, danh sách vùng nguy hiểm                                                          |
| 👥 Nhóm    | Groups          | Danh sách nhóm; chi tiết nhóm (khoản chi, số dư, sổ nợ, nhắc nợ)                                   |

**Màn hình phụ (Stack / Modal):**

- Onboarding (4 bước): chế độ thu nhập + thu nhập → ngày nhận tiền (nếu lương cố định) → khoản cố định → mục tiêu tiết kiệm
- Đăng nhập / đăng ký
- Đồng ý quyền riêng tư khi dùng AI (F09), hiện trước lần ghi bằng giọng nói / ảnh đầu tiên
- Xác nhận giao dịch từ AI (hiện transcript / ảnh gốc, các ô sửa được)
- Báo cáo & ngân sách
- Khoản định kỳ & đăng ký (subscriptions)
- Mục tiêu tiết kiệm
- Trợ lý AI (chat)
- Thêm khoản chi nhóm / chia bill
- Màn hình VietQR (trả nợ / nhận nợ)
- Tham gia nhóm bằng mã / quét QR
- Cài đặt: ví, danh mục, tài khoản ngân hàng nhận tiền, lương theo giờ, quyền (vị trí, thông báo, đọc thông báo), giao diện

---

## 10. Cấu trúc thư mục

```
vi-luoi/
├── app/                          # Expo Router
│   ├── (auth)/
│   │   ├── sign-in.tsx
│   │   └── sign-up.tsx
│   ├── (onboarding)/
│   ├── (tabs)/
│   │   ├── index.tsx             # Hôm nay
│   │   ├── transactions/
│   │   ├── map.tsx
│   │   └── groups/
│   ├── capture/                  # modal ghi chi tiêu
│   │   ├── voice.tsx
│   │   ├── image.tsx
│   │   └── confirm.tsx
│   ├── reports.tsx
│   ├── recurring.tsx
│   ├── assistant.tsx
│   └── _layout.tsx
├── src/
│   ├── features/                 # Mỗi feature: api / hooks / components / utils
│   │   ├── auth/
│   │   ├── transactions/
│   │   ├── capture/
│   │   ├── budget/               # safe-to-spend, ngân sách, mục tiêu
│   │   ├── location/             # bản đồ, geofence, background task
│   │   ├── groups/               # quỹ nhóm, chia bill, sổ nợ
│   │   ├── vietqr/
│   │   └── notifications/
│   ├── components/ui/            # Button, Card, MoneyText, AmountInput...
│   ├── lib/
│   │   ├── supabase.ts
│   │   ├── db.ts                 # expo-sqlite + offline queue
│   │   └── query-client.ts
│   ├── stores/                   # Zustand
│   ├── types/                    # Kiểu sinh từ Supabase (supabase gen types)
│   └── utils/                    # format tiền, ngày, chuẩn hóa chuỗi tiếng Việt
├── supabase/
│   ├── migrations/               # SQL schema + RLS + functions
│   ├── functions/
│   │   ├── parse-voice/
│   │   ├── parse-text/
│   │   ├── parse-image/
│   │   ├── ai-assistant/
│   │   ├── detect-recurring/
│   │   └── send-push/
│   └── seed.sql                  # Danh mục mặc định + dữ liệu demo
├── __tests__/
├── app.config.ts
├── eas.json
└── README.md
```

---

## 11. Phân công 6 thành viên

Mỗi người sở hữu **một feature từ đầu đến cuối** (UI + logic + phần backend liên quan), để ai cũng có phần frontend lẫn backend khi bảo vệ.

| Thành viên | Vai trò                 | Phụ trách                                                                                                                                                                        | Tính năng                              |
| ---------- | ----------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------- |
| **TV1**    | Trưởng nhóm / Tech Lead | Khởi tạo dự án, file quy ước AI, Expo Router, design system (NativeWind), Supabase setup, EAS Build, push notification (FCM + `send-push`), màn Cài đặt, gộp nhánh, ghép báo cáo | F01, F02, khung điều hướng, Onboarding |
| **TV2**    | Core Data + chủ schema  | Ví, danh mục, giao dịch CRUD, lịch sử, số dư ví; **duyệt mọi migration**; cùng TV6 viết `fn_apply_group_expense`; kiểm thử RLS, seed dữ liệu demo                                | F03–F06; nếu kịp: F07                  |
| **TV3**    | AI Capture              | Ghi âm, Edge Functions `parse-voice` / `parse-text`, prompt + schema, màn xác nhận dùng chung, học thói quen, màn quyền riêng tư AI, **bộ dữ liệu đánh giá AI**                  | F09, F10, F11, F15                     |
| **TV4**    | Image & Location        | Camera / chọn ảnh, `parse-image`, gắn vị trí, bản đồ chi tiêu, slide thuyết trình                                                                                                | F12, F30, F31; nếu kịp: F13, F32       |
| **TV5**    | Budget & Insights       | Safe-to-spend (2 chế độ thu nhập), màn Home, ngân sách, khoản định kỳ, nhắc gia hạn, quy đổi giờ làm, thu phản hồi người dùng, video demo                                        | F20–F23, F51, F53; nếu kịp: F24, F25   |
| **TV6**    | Social                  | Quỹ nhóm realtime, chia bill, sổ nợ, VietQR, nhắc nợ, xác nhận trả                                                                                                               | F40–F45                                |

> Việc chi tiết của từng người, kèm hạn và giờ ước lượng, nằm ở sheet [Phân công Ví Lười 3 tuần](https://claude.ai/artifact/CpdnXJgfMP6nKvBSwB1bSK). Bản đồ và geofence thuộc TV4 vì phần Nhóm đã đủ nặng cho TV6.

**Trách nhiệm chung:**

- Mỗi người viết test cho phần logic của mình.
- Mỗi người viết phần báo cáo cho feature mình làm.
- Mỗi người review ít nhất 2 PR của người khác mỗi tuần. Review xoay vòng, không dồn hết cho TV1.

---

## 12. Lộ trình phát triển

> **Lịch: 3 tuần (21 ngày), có dùng AI để viết code.** Bảng việc chi tiết (88 việc, người làm, hạn, phụ thuộc, trạng thái) nằm ở sheet [Phân công Ví Lười 3 tuần](https://claude.ai/artifact/CpdnXJgfMP6nKvBSwB1bSK). Mục này chỉ ghi mục tiêu từng tuần; khi hai nơi lệch nhau thì theo sheet.
> Ngày 1 tạm đặt là thứ Hai 23/11/2026; đổi ngày bắt đầu trong sheet thì mọi hạn tự dời theo. Mỗi người dự kiến khoảng 40–55 giờ trong 3 tuần, cộng khoảng 11 giờ việc chung.

### Các mốc

| Mốc                     | Ngày    | Kiểm tra bằng                                                                                                                                                           |
| ----------------------- | ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Mốc 1 · Nền móng        | Ngày 7  | Đăng ký → onboarding → thêm khoản chi tay → safe-to-spend giảm                                                                                                          |
| Mốc 2 · Tính năng chính | Ngày 14 | Nói _"trà sữa 35k"_ → có giao dịch → safe-to-spend giảm; chụp ảnh chuyển khoản ra giao dịch; 2 máy thấy khoản chi nhóm cùng lúc, mỗi người chỉ giảm đúng phần mình chịu |
| Feature freeze          | Ngày 17 | Đủ các tính năng "Làm" ở [mục 4.7](#47-phạm-vi-mvp-bắt-buộc-hoàn-thành); từ sau ngày này chỉ sửa lỗi                                                                    |
| Nộp bài                 | Ngày 21 | Đủ [mục 17](#17-sản-phẩm-bàn-giao)                                                                                                                                      |

### Ngày 1–3 — Chuẩn bị (nằm trong tuần 1)

- [ ] Ngày 1: họp khởi động, chốt phạm vi và phân công; tạo repo và file quy ước chung cho AI (`AGENTS.md` / `CLAUDE.md`, xem [mục 13](#dùng-ai-để-viết-code))
- [ ] Ngày 2: **chốt thiết kế** 2 chế độ thu nhập và công thức safe-to-spend ([8.1](#81-safe-to-spend--còn-được-tiêu-hôm-nay)), cách chi nhóm tính vào chi cá nhân ([mục 6](#chi-tiêu-nhóm-và-chi-tiêu-cá-nhân))
- [ ] Ngày 2: dọn sạch project Supabase cũ, tạo Firebase project cho FCM, lấy API key Gemini
- [ ] Ngày 3: cả 6 máy Android chạy được development build; xác nhận có ai có iPhone và tài khoản Apple Developer không
- Bỏ bước wireframe Figma: dựng thẳng giao diện bằng design system (việc của TV1, xong ngày 4).

### Tuần 1 (ngày 1–7) — Nền móng

| TV  | Việc                                                                                                                      |
| --- | ------------------------------------------------------------------------------------------------------------------------- |
| TV1 | Repo, file quy ước AI, dev build, khung điều hướng, design system, đăng nhập email, onboarding                            |
| TV2 | Chốt thiết kế dữ liệu, migration + RLS, hook dữ liệu, ví / danh mục, form nhập tay                                        |
| TV3 | Thử Gemini structured output, bộ 50 câu đánh giá, Edge Function `parse-text`, hàm tách số tiền tiếng Việt                 |
| TV4 | Chụp / chọn ảnh + upload, gom 20 ảnh test, `parse-image` bản đầu, thử geofence trên máy thật (quyết định làm hay cắt F32) |
| TV5 | Hàm safe-to-spend + unit test, hook `useSafeToSpend`, màn Home                                                            |
| TV6 | Migration nhóm + RLS, tạo / tham gia nhóm bằng mã, thử Supabase Realtime                                                  |

### Tuần 2 (ngày 8–14) — Tính năng chính

| TV  | Việc                                                                                                                 |
| --- | -------------------------------------------------------------------------------------------------------------------- |
| TV1 | Push notification (FCM, `send-push`), màn Cài đặt (tài khoản nhận tiền, lương giờ), gộp nhánh hằng ngày, APK thử     |
| TV2 | Lịch sử + lọc / tìm kiếm, số dư ví, `fn_apply_group_expense` (cùng TV6), hàng đợi gửi lại khi mất mạng (nếu kịp)     |
| TV3 | Ghi âm, `parse-voice`, màn xác nhận dùng chung, gõ câu nhanh, màn quyền riêng tư AI, học thói quen, đo lại bộ 50 câu |
| TV4 | Luồng ảnh hoàn chỉnh (nhiều dòng), gắn vị trí khi ghi, bản đồ chi tiêu                                               |
| TV5 | Ngân sách theo danh mục, khoản định kỳ + nhắc gia hạn, quy đổi giờ làm, kiểm thử safe-to-spend khi có chi nhóm       |
| TV6 | Thêm khoản chi nhóm (chia đều, theo số tiền), danh sách realtime, nối giao dịch cá nhân, thuật toán rút gọn nợ       |

### Tuần 3 (ngày 15–21) — Hoàn thiện và nộp

**Ngày 15–17: làm nốt tính năng**

| TV  | Việc                                                                                               |
| --- | -------------------------------------------------------------------------------------------------- |
| TV1 | Push nhắc nợ mở thẳng màn VietQR; rà trạng thái đang tải / lỗi / rỗng / từ chối quyền trên mọi màn |
| TV2 | Script kiểm thử RLS; seed dữ liệu demo cho tài khoản demo                                          |
| TV3 | Xử lý lỗi luồng AI; tinh chỉnh prompt, chốt số liệu đánh giá                                       |
| TV4 | Chia sẻ ảnh vào app (nếu kịp); vùng nguy hiểm + geofence (nếu bản thử tuần 1 ổn)                   |
| TV5 | Biểu đồ chi tiêu (nếu kịp); ca biên safe-to-spend; mục tiêu tiết kiệm (nếu kịp)                    |
| TV6 | VietQR, nút nhắc nợ, xác nhận đã trả, kiểm thử nhóm trên 3 máy                                     |

**Ngày 18–21: ổn định, báo cáo, bảo vệ**

- [ ] Ngày 18: test chéo theo checklist, mỗi người test màn của người khác
- [ ] Ngày 19: sửa lỗi; 10–20 bạn dùng thử APK và trả lời Google Form (TV5); cập nhật kịch bản demo theo tính năng thật sự có (TV1)
- [ ] Ngày 20: APK cuối (TV1), video demo (TV5), slide (TV4); mỗi người nộp phần báo cáo của mình, TV1 ghép
- [ ] Ngày 21: tập thuyết trình 2 lần, nộp bài

> **Khi trễ, cắt theo thứ tự:** mục tiêu tiết kiệm → gợi ý vùng tự động → biểu đồ chi tiêu → chia sẻ ảnh vào app → geofence → hàng đợi gửi lại. **Không cắt:** safe-to-spend, ghi bằng giọng nói, quét ảnh, chi nhóm, sổ nợ, VietQR.

---

## 13. Quy trình làm việc nhóm

### Git

- Nhánh: `main` (luôn build được) ← `feature/<mã việc>-<ten-ngan>`. Với 3 tuần, bỏ nhánh `develop` cho đỡ một bước gộp.
  Ví dụ: `feature/V39-parse-voice`, mã việc lấy từ sheet phân công.
- Commit theo **Conventional Commits**: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`
- Mọi thay đổi vào `main` qua **Pull Request** nhỏ, cần ít nhất 1 người duyệt. TV1 gộp nhánh hằng ngày, không để PR chờ quá 1 ngày.
- Không commit file `.env`, key API, file build.

### Database

- Mọi thay đổi schema viết thành file trong `supabase/migrations/`, **không** sửa tay trên dashboard.
- Sau khi đổi schema, chạy `supabase gen types typescript` và commit file kiểu.
- **TV2 là chủ schema:** mọi PR có migration cần TV2 duyệt. Tạo file bằng `supabase migration new <ten>` ngay trước khi mở PR; nếu bị trùng thứ tự với migration vừa merge thì đổi timestamp và rebase.
- Mọi người dùng chung một project cho cả dev và demo; chỉ đẩy lên project migration đã merge vào `main` (giới hạn sau feature freeze: mục 5.4).

### Họp nhóm

- **Họp đầu tuần** (15 phút, ngày 1, 8, 15): xem sheet, chỉnh việc trễ.
- **Mỗi tối:** cập nhật cột Trạng thái trong sheet và nhắn nhóm chat theo mẫu _Hôm nay xong gì / mai làm gì / đang vướng gì_. Việc bị kẹt thì đổi sang "Bị chặn" ngay.
- **Demo ở mỗi mốc** (30 phút, ngày 7 và 14): mỗi người demo phần của mình trên máy thật.

### Dùng AI để viết code

Bản đầy đủ có lý do ở tab "Dùng AI" của sheet phân công. Tóm tắt:

1. Mọi phiên AI đọc chung một file quy ước trong repo (`AGENTS.md` / `CLAUDE.md`).
2. Mỗi prompt dán kèm đúng đoạn kế hoạch liên quan và cột "Kết quả cần có" của việc.
3. Một việc là một nhánh, một PR nhỏ (dưới khoảng 400 dòng).
4. Mỗi người phải hiểu và giải thích được mọi dòng trong PR của mình. Khi bảo vệ, giảng viên hỏi người làm.
5. Công thức và thuật toán (safe-to-spend, rút gọn nợ, CRC16, chia bill): tự tính tay ví dụ trước, rồi mới cho AI viết test và code.
6. SQL và RLS do AI viết phải có test truy cập chéo do người viết.
7. Không dán `.env`, API key, mật khẩu, ảnh chuyển khoản chưa che vào chat với AI.
8. Không để AI sửa migration đã gộp; thư viện mới phải hỏi TV1 trước khi cài.
9. Kẹt với AI quá 30 phút thì hỏi người trong nhóm.
10. Khai báo việc dùng AI trong báo cáo theo quy định của môn.

### Definition of Done (một task được coi là xong khi)

- [ ] Chạy được trên Android (và iOS nếu có thiết bị)
- [ ] Có xử lý loading, lỗi, trạng thái rỗng
- [ ] Logic nghiệp vụ có unit test
- [ ] PR đã được duyệt và merge vào `main`
- [ ] Người làm giải thích được code, kể cả phần AI viết
- [ ] Đã đổi Trạng thái trong sheet sang "Xong"
- [ ] Đã cập nhật tài liệu / README nếu cần

---

## 14. Kiểm thử

| Loại              | Công cụ                      | Phạm vi                                                                                                                                                                                                                       |
| ----------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Unit test         | Jest                         | Safe-to-spend, rút gọn nợ, sinh VietQR + CRC16, chuẩn hóa merchant, phát hiện định kỳ, format tiền                                                                                                                            |
| Component test    | React Native Testing Library | Form nhập tiền, màn xác nhận AI                                                                                                                                                                                               |
| Kiểm thử AI       | Bộ dữ liệu tự tạo            | **50 câu nói tiếng Việt + 20 ảnh chuyển khoản** của nhiều ngân hàng; đo % trích xuất đúng số tiền / danh mục. Đo lần đầu ngày 5, đo lại ngày 14 và chốt ngày 18 để thấy độ chính xác tăng dần. **Đưa con số này vào báo cáo** |
| Kiểm thử RLS      | SQL test / script            | User A không đọc được dữ liệu của User B; người ngoài nhóm không đọc được dữ liệu nhóm; thành viên nhóm không đọc được thu nhập của nhau                                                                                      |
| Kiểm thử chi nhóm | SQL test                     | Tổng phần safe-to-spend giảm của cả nhóm bằng tổng khoản chi; trả nợ không làm safe-to-spend thay đổi; sửa / xóa khoản chi nhóm sinh lại đúng giao dịch cá nhân                                                               |
| E2E (tùy chọn)    | Maestro                      | Luồng đăng nhập → ghi chi tiêu → xem Home                                                                                                                                                                                     |
| Thủ công          | Checklist                    | Mất mạng, từ chối quyền, app bị kill khi đang ghi âm, geofence khi app chạy nền (thử cả máy Xiaomi / Oppo nếu có)                                                                                                             |
| Người dùng thật   | Google Form                  | 10–20 người dùng thử APK 2 ngày (ngày 18–19): thời gian trung bình để ghi 1 khoản, mức độ hài lòng                                                                                                                            |

**Các test case quan trọng cho AI:**

- `"35k"`, `"35 nghìn"`, `"ba lăm nghìn"`, `"35 ca"`, `"1 xị"`, `"2 lít"` (100k, 200k), `"1 củ"` (1 triệu)
- `"hôm qua"`, `"sáng nay"`, `"thứ 7 tuần trước"`
- Một câu nhiều khoản: `"cơm 30k với nước 15k"`
- Thu nhập: `"mẹ cho 2 triệu"`, `"lương về"`

---

## 15. Rủi ro và phương án dự phòng

| Rủi ro                                                       | Khả năng   | Ảnh hưởng  | Phương án                                                                                                                                                      |
| ------------------------------------------------------------ | ---------- | ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| AI trích xuất sai số tiền                                    | Trung bình | Cao        | Luôn có màn xác nhận; highlight trường độ tin cậy thấp; quy tắc regex cho số tiền làm lớp kiểm tra thứ hai                                                     |
| Hết hạn mức gói miễn phí API AI                              | Thấp       | Trung bình | Cache theo câu; khớp `merchant_rules` trước khi gọi AI; chuẩn bị key dự phòng                                                                                  |
| Đọc thông báo không làm được trên iOS                        | Chắc chắn  | Thấp       | Xác định từ đầu là tính năng **chỉ Android**, P2; iOS dùng Share ảnh thay thế                                                                                  |
| Geofence chạy nền không ổn định                              | Trung bình | Trung bình | Thử trên máy thật ngay tuần 1 (hạn ngày 7), không ổn thì cắt F32; hướng dẫn tắt tối ưu pin trên ROM Xiaomi / Oppo / Vivo; dự phòng: kiểm tra vị trí khi mở app |
| Không có thiết bị / tài khoản để build iOS                   | Cao        | Thấp       | Chốt Android là nền tảng chính từ đầu; iOS là điểm cộng                                                                                                        |
| Dữ liệu nhạy cảm (ảnh chuyển khoản, số tài khoản) gửi cho AI | Chắc chắn  | Trung bình | Màn đồng ý F09; nói rõ trong báo cáo; dùng gói trả phí nếu có credit vì gói miễn phí có thể dùng dữ liệu để huấn luyện                                         |
| Safe-to-spend tính sai khi có chi nhóm (trừ hai lần)         | Trung bình | Cao        | Chốt thiết kế ở [mục 6](#chi-tiêu-nhóm-và-chi-tiêu-cá-nhân) ngày 2; có SQL test riêng                                                                          |
| Project Supabase gói miễn phí bị tạm dừng                    | Trung bình | Cao        | Kiểm tra trước mỗi buổi demo và ngày bảo vệ; có video dự phòng                                                                                                 |
| Ôm quá nhiều tính năng                                       | Cao        | Cao        | Phạm vi 3 tuần ở mục 4.7; việc "Nếu kịp" không nằm trong cam kết; feature freeze ngày 17; thứ tự cắt ghi sẵn ở mục 12                                          |
| Trễ một việc kéo theo cả chuỗi (lịch chỉ 21 ngày)            | Cao        | Cao        | Cột Phụ thuộc trong sheet; cập nhật trạng thái mỗi tối; việc "Bị chặn" được xử lý trong ngày                                                                   |
| Code do AI viết mà người làm không hiểu, hoặc sai ở ca biên  | Cao        | Cao        | PR nhỏ, review chéo; tự tính tay ví dụ cho công thức trước khi để AI viết test; người làm phải giải thích được PR                                              |
| Code của 6 người không đồng bộ vì mỗi người dùng AI một kiểu | Trung bình | Trung bình | File quy ước chung `AGENTS.md` / `CLAUDE.md` từ ngày 1; design system và hook mẫu có sẵn trước khi làm màn                                                     |
| Expo Go không chạy native module                             | Chắc chắn  | Trung bình | Dùng **development build** ngay từ ngày 3                                                                                                                      |
| Thành viên trễ việc                                          | Trung bình | Cao        | Board cập nhật hằng ngày; P2 là vùng đệm, có thể cắt; tech lead hỗ trợ                                                                                         |
| Xung đột code khi merge                                      | Trung bình | Trung bình | Chia theo feature folder; PR nhỏ, merge thường xuyên                                                                                                           |
| Demo lỗi mạng tại lớp                                        | Trung bình | Cao        | Quay video demo dự phòng; dữ liệu demo đã seed sẵn; dùng 4G cá nhân                                                                                            |
| Lộ API key                                                   | Thấp       | Cao        | Key chỉ nằm trong secrets Edge Function, `.env` trong `.gitignore`                                                                                             |

---

## 16. Kịch bản demo (khoảng 7 phút)

1. **Mở đầu (30s):** "Có ai từng cài app ghi chi tiêu rồi bỏ sau 1 tuần không?" → nêu vấn đề _lười nhập_.
2. **Home (30s):** con số _"Hôm nay còn được tiêu 82.000đ"_, giải thích ngắn công thức.
3. **Ghi bằng giọng nói (1 phút):** bấm mic nói _"trà sữa 35 nghìn với bánh 20k"_ → 2 giao dịch xuất hiện → con số giảm còn 27.000đ, chuyển màu vàng.
4. **Ghi bằng ảnh (1 phút):** mở app ngân hàng → chụp màn hình chuyển khoản → _Chia sẻ → Ví Lười_ → giao dịch tự điền.
5. **Ngân sách (30s):** mở báo cáo / ngân sách theo danh mục, chỉ ra danh mục Mua sắm đã dùng 85%. _Nếu F14 (P2) đã xong, thay bằng: giả lập một thông báo ngân hàng → giao dịch chờ xác nhận hiện trên Home._
6. **Bản đồ (1 phút):** mở bản đồ chi tiêu → bấm vào cụm chi nhiều nhất xem tổng chi tại đó. _Nếu geofence (F32) đã xong, thêm: dùng vị trí giả lập (mock location) trên Android để đi vào "vùng nguy hiểm" → thông báo cảnh báo hiện lên._
7. **Quỹ nhóm (1,5 phút):** 2 điện thoại. Máy A thêm _"Lẩu 600k chia 3"_ → máy B thấy ngay, safe-to-spend của B chỉ giảm 200k (phần mình chịu) → sổ nợ rút gọn → máy A bấm _Nhắc_ → máy B nhận push kèm VietQR → quét bằng app ngân hàng thật.
8. **Kết (30s):** số liệu kiểm thử (_"AI đúng 9x% trên 50 câu thử"_, _"thời gian ghi 1 khoản giảm từ ~15s xuống ~3s"_), hướng phát triển.

---

## 17. Sản phẩm bàn giao

- [ ] Mã nguồn trên GitHub (README hướng dẫn chạy, file `.env.example`)
- [ ] File APK cài đặt (EAS Build)
- [ ] Thư mục `supabase/` (migrations, functions, seed) để dựng lại backend
- [ ] Báo cáo đồ án: giới thiệu, phân tích yêu cầu, thiết kế (kiến trúc, ERD, use case, sequence diagram cho luồng AI và chia bill), cài đặt, kiểm thử, kết luận
- [ ] Slide thuyết trình
- [ ] Video demo (3–5 phút)
- [ ] Bảng phân công và đánh giá đóng góp từng thành viên

---

### Hướng phát triển sau đồ án

- Kết nối trực tiếp ngân hàng qua API Open Banking (khi có hạ tầng chính thức ở Việt Nam)
- Dự báo chi tiêu cuối tháng bằng mô hình chuỗi thời gian
- Widget iOS, Apple Watch / Wear OS: ghi chi tiêu bằng giọng nói trên đồng hồ
- Chế độ gia đình: bố mẹ cấp "tiền tiêu vặt" và theo dõi cho con
