# 9. Chiến lược kiểm thử

## 9.1 Các tầng

| Tầng               | Công cụ                                      | Phủ cái gì                                                                                                                                                             | Lệnh                        | Khi nào bắt buộc                           |
| ------------------ | -------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------- | ------------------------------------------ |
| Unit server        | JUnit 5, AssertJ                             | Hàm thuần ở `domain` (danh sách 8.2). Đáp án tính tay trước (ví dụ ở 4.2, 2.3)                                                                                         | `./mvnw test`               | Mọi PR sửa `domain`                        |
| Truy cập chéo      | `@SpringBootTest` + MockMvc + Testcontainers | 23 kịch bản X1–X23 ở 3.4                                                                                                                                               | `./mvnw verify`             | Mọi PR thêm/sửa endpoint có quyền          |
| Tích hợp server    | Như trên                                     | Repository + SQL thật (pgvector, PostGIS): tìm kiếm, upsert match, partial unique index, `FOR UPDATE SKIP LOCKED`, chuyển trạng thái đồng thời (2 luồng cùng `accept`) | `./mvnw verify`             | PR sửa SQL / migration                     |
| Đánh giá đối chiếu | JUnit `@Tag("eval")`                         | top-1, top-5, MRR trên bộ mẫu                                                                                                                                          | `./mvnw test -Dgroups=eval` | Khi đổi trọng số/model; số liệu dán vào PR |
| Unit app           | Jest (`jest-expo`)                           | Hàm thuần ở `utils/` (8.1)                                                                                                                                             | `npm test`                  | Mọi PR                                     |
| Kiểm tra app       | ESLint, `tsc`                                |                                                                                                                                                                        | `npm run check`             | Mọi PR                                     |
| Thủ công trên máy  | 2 máy Android thật                           | Kịch bản demo 9.3                                                                                                                                                      |                             | Trước khi gộp màn mới (kèm ảnh chụp)       |

Không viết test giao diện tự động (E2E): 3 tuần không đủ để giữ chúng ổn định; kịch bản demo thủ công thay thế.

## 9.2 Quy ước test server

- **Testcontainers** dùng image `supabase/postgres` (có sẵn pgvector + PostGIS, giống production) _(giả định U9; nếu không chạy được thì dùng Dockerfile `FROM postgis/postgis:16-3.4` + `apt-get install postgresql-16-pgvector`)_. Flyway chạy migration thật lên container.
- Test tích hợp đặt tên `*IT` / `*AccessTest`, chạy ở pha `verify` (Failsafe). `./mvnw test` chỉ chạy unit nên ai không có Docker vẫn chạy được.
- `EmbeddingClient`, `SupabaseStorageClient`, `ExpoPushClient` thay bằng bản giả trong test (`@TestConfiguration`). Không test gọi dịch vụ thật.
- Thời gian lấy từ `Clock` tiêm vào, không gọi `Instant.now()` trực tiếp, để test hết hạn QR, BR-07, BR-09.
- Fixture: `TestUsers.a()`, `b()`, `c()`, `admin()` tạo user + JWT của server trực tiếp. `IdentityVerifier` thay bằng bản giả, trừ test X23 dùng `FirebaseTokenVerifier` thật với JWKS giả (khóa RSA sinh trong test) để kiểm tra chữ ký, `iss`, `aud`, hạn.
- CI: một workflow GitHub Actions chạy `npm run check` và `./mvnw verify` (runner Ubuntu có Docker).

## 9.3 Kịch bản demo (5–7 phút, máy A = Finder, máy B = Owner)

Chuẩn bị: nạp dữ liệu mẫu (`./mvnw spring-boot:run -Dspring-boot.run.arguments=--seed`, nạp 60 tin kèm vector có sẵn), đánh thức server 10 phút trước, đăng nhập sẵn hai máy (một máy bằng Google, một máy bằng số điện thoại thử của Firebase để không phụ thuộc SMS).

| #   | Máy                       | Thao tác                                                             | Điểm cần nói                             |
| --- | ------------------------- | -------------------------------------------------------------------- | ---------------------------------------- |
| 1   | B                         | Mở bản đồ, lọc "Tai nghe"                                            | Vị trí tin nhặt được làm tròn (BR-11)    |
| 2   | B                         | Tìm bằng chữ "tai nghe trắng hộp sứt góc"                            | Điểm + lý do; công thức 4.2              |
| 3   | B                         | Đăng tin mất "tai nghe trắng", bán kính 200 m                        |                                          |
| 4   | A                         | Đăng tin nhặt được: chụp tai nghe thật, câu hỏi kín "Hộp có dán gì?" | Đăng < 1 phút; không chờ AI              |
| 5   | B                         | Nhận push "Có món giống đồ bạn mất"                                  | Đối chiếu nền, ngưỡng 0,65               |
| 6   | B                         | "Đây là đồ của tôi", trả lời câu hỏi                                 | Đáp án không bao giờ rời server tới B    |
| 7   | A                         | Xem câu trả lời, chấp nhận                                           | Finder quyết (BR-03); máy trạng thái 5.2 |
| 8   | A ↔ B                     | Chat hẹn gặp                                                         | Không lộ email (BR-04)                   |
| 9   | A → B                     | A hiện QR, B quét → "Đã trả"                                         | Mã 1 lần, 10 phút; uy tín A +10          |
| 10  | C (máy B, tài khoản khác) | Thử mở lại claim cũ                                                  | 404, test X11                            |

Dự phòng: server chậm → banner chờ (7.5); Render hỏng → server trên laptop + màn đổi URL; Jina hỏng → bước 2 hạ cấp sang lọc chữ; bước 5 thay bằng FR-MATCH-06: A mở tin mất của B, bấm "Tôi nhặt được món này" (không cần AI), B vẫn nhận thông báo và đi tiếp từ bước 6 (NFR-07).
