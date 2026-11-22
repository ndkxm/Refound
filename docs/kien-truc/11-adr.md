# 11. Bản ghi quyết định (ADR)

Mỗi ADR: bối cảnh → quyết định → hệ quả → phương án đã loại. Trạng thái "Đề xuất" nghĩa là nhóm chưa xác nhận.

---

### ADR-01. Đối chiếu nền chạy trong tiến trình Spring, hàng đợi là cột của `items`

**Đề xuất.** FR-MATCH-03, NFR-05.

- **Bối cảnh:** cần chạy embedding + đối chiếu sau khi đăng, chịu được lỗi AI và việc Render tắt server bất cứ lúc nào. Một instance, vài trăm tin.
- **Quyết định:** `embedding_status / embedding_attempts / next_embed_at` trên `items` là hàng đợi. Job chạy ngay sau commit (`@TransactionalEventListener` + executor 1 luồng) và `@Scheduled` mỗi 30 s vét lại; lấy việc bằng `FOR UPDATE SKIP LOCKED`.
- **Hệ quả:** không thêm dịch vụ; trạng thái job nằm trong DB nên server chết không mất việc; giải thích được bằng một câu SQL. Server ngủ thì job dừng, nhưng việc chỉ sinh ra khi có request, mà request đánh thức server.
- **Đã loại:** trigger Postgres (không gọi HTTP ra ngoài được một cách an toàn), hàng đợi ngoài (Redis/RabbitMQ: thêm dịch vụ, không free lâu dài), bảng `jobs` tổng quát (chỉ có một loại việc).

### ADR-02. Embedding: `jina-clip-v2` 768 chiều; dự phòng Gemini (Q3)

**Đề xuất, chốt sau thử nghiệm 4.6.**

- **Bối cảnh:** cần so chữ↔ảnh (tin mất thường chỉ có chữ); số chiều phải chốt trước migration.
- **Quyết định:** mặc định `jina-clip-v2` với `dimensions=768`. Số chiều **768** chốt ngay vì cả `jina-clip-v2` (Matryoshka) và `gemini-embedding-001` (`output_dimensionality`) đều cắt được về 768 _(U3)_. Code gọi qua interface `EmbeddingClient`; cột `model` ghi model đã dùng. Groq chỉ dùng trong phương án B (mô tả ảnh thành chữ).
- **Hệ quả:** migration không chờ Q3; đổi model chỉ cần đặt lại `pending` và chạy lại job. Ở phương án A, Groq không có vai trò bắt buộc (van-de V8).
- **Đã loại:** 1024 chiều (Gemini không có mức đó _(U3)_); lưu vector nhiều độ dài khác nhau (phức tạp, không so được với nhau).

### ADR-03. Không tạo chỉ mục vector (HNSW/IVFFlat)

**Đề xuất.**

- **Bối cảnh:** vài trăm tin × ~3 vector. Truy vấn luôn kèm bộ lọc (loại, trạng thái, khoảng cách, thời gian).
- **Quyết định:** quét tuần tự, tính cosine chính xác.
- **Hệ quả:** kết quả chính xác 100%, không có lỗi "lọc sau HNSW trả thiếu kết quả"; vẫn nhanh (ước lượng < 50 ms). Khi > ~10.000 vector thì thêm `CREATE INDEX ... USING hnsw (embedding vector_cosine_ops)` bằng migration mới.
- **Đã loại:** HNSW ngay từ đầu (gần đúng, thêm tham số phải giải thích, không cần ở quy mô này).

### ADR-04. Phân quyền: kiểm tra sở hữu ở service bằng hàm thuần `*Access`

**Đề xuất.** NFR-01, R10.

- **Quyết định:** Spring Security lo xác thực JWT và vai trò admin theo URL (+ `@PreAuthorize("hasRole('ADMIN')")` trên controller admin). Mọi kiểm tra "bản ghi này có phải của tôi / tôi có phải một bên của claim" nằm ở `XxxAccess.requireYyy(record, me)` gọi đầu mỗi method service. DTO tách theo người xem.
- **Hệ quả:** đọc một method service là thấy đủ quyền; test hàm thuần không cần Spring; test truy cập chéo vẫn bắt buộc vì quên gọi `require` là lỗi phổ biến nhất.
- **Đã loại:** SpEL `@PreAuthorize("@claimSecurity.isParticipant(#id, principal)")` (khó đọc, khó test, phải truy DB hai lần); RLS Postgres với `SET LOCAL` user id (hai nơi định nghĩa quyền, khó debug qua pooler).

### ADR-05. Đăng nhập bằng Google hoặc số điện thoại (Firebase Auth); server tự phát hành JWT

**Chốt ngày 22/11/2026 (nhóm quyết).** Thay cho "email bất kỳ, không xác minh" ở FR-AUTH-01 và BRD 5.2. BRD đã sửa theo (v0.2).

- **Bối cảnh:** đăng ký bằng email không xác minh thì tạo tài khoản giả quá dễ (van-de V5) và quên mật khẩu là mất tài khoản (V6). Nhóm muốn người dùng **chọn một trong hai**: đăng nhập Google (OAuth) hoặc xác minh số điện thoại qua SMS.
- **Quyết định:**
  1. App dùng **Firebase Auth** cho cả hai cách: `@react-native-google-signin/google-signin` lấy credential Google → `signInWithCredential`; số điện thoại → `signInWithPhoneNumber` + mã OTP. Project Firebase đã có sẵn vì dùng cho push (FCM).
  2. App gửi **Firebase ID token** lên `POST /auth/firebase`. Server kiểm tra token bằng `NimbusJwtDecoder.withJwkSetUri(<khóa công khai của Google cho securetoken>)` có sẵn trong Spring Security: chữ ký RS256, `iss = https://securetoken.google.com/<projectId>`, `aud = <projectId>`, còn hạn, `firebase.sign_in_provider ∈ {google.com, phone}` _(U18)_. Không cần Firebase Admin SDK.
  3. Server tìm hoặc tạo `users` theo `firebase_uid` rồi phát hành **JWT của riêng mình** (HS256, `JWT_SECRET`, `sub=userId`, `role`, hạn 7 ngày). Mọi API khác chỉ dùng JWT này. Mỗi request kiểm tra `users.status='active'` (một truy vấn theo PK).
  4. Server **không lưu** email, SĐT hay mật khẩu; chỉ lưu `firebase_uid` và `auth_provider`. Tên hiển thị do người dùng nhập (gợi ý sẵn tên Google).
  5. Phát triển và demo dùng **số điện thoại thử** khai báo trong Firebase console (mã OTP cố định, không gửi SMS, không tốn tiền).
- **Hệ quả:** không có bảng mật khẩu, không có luồng quên mật khẩu, không phải chống dò mật khẩu. Tạo tài khoản giả khó hơn: mỗi tài khoản cần một tài khoản Google hoặc một SIM. Thêm 3 thư viện native (8.1) và bước khai báo SHA-1/SHA-256 của khóa ký APK trong Firebase. **Rủi ro chi phí:** SMS thật có thể không nằm trong gói miễn phí của Firebase _(U16)_. Nếu vậy, bản demo vẫn chạy bằng số thử và Google; SMS thật chỉ bật khi xác nhận được là miễn phí.
- **Đã loại:** tự gửi SMS qua nhà mạng/Twilio (tốn tiền); Supabase Auth (app phải gọi thẳng Supabase); Google OAuth qua `expo-auth-session` + SMS qua Firebase (hai đường xác minh ở server thay vì một); refresh token (thêm bảng và luồng không cần cho 6–10 người).

### ADR-06. Ảnh: resize ở app, upload qua server, đọc bằng signed URL

**Chốt ngày 22/11/2026** (nhóm giao kiến trúc sư chọn phương án tối ưu). Q5, BR-08, NFR-02.

- **Quyết định:** app tạo 2 bản JPEG (lớn 1024px ~ chất lượng 0,7; nhỏ 512px ~ 0,6) và gửi multipart lên server; server kiểm tra (JPEG, ≤ 1 MB, ≤ 4 ảnh) rồi upload Storage bằng service key. Đọc ảnh: server trả signed URL hết hạn 1 giờ (ký theo lô một lần gọi); app tải ảnh trực tiếp từ URL đó. Ảnh nhạy cảm chỉ được ký cho người có quyền (3.2).
- **Hệ quả:** RAM server không phải giải mã ảnh; ảnh không đi qua Render khi đọc (đỡ băng thông, đỡ chờ server thức). URL ký không chứa khóa, chỉ mở được đúng một file trong 1 giờ, và chỉ server quyết ai được cấp. Vì vậy nguyên tắc "app không gọi thẳng Supabase" được hiểu là: app không giữ khóa, không gọi API dữ liệu/Storage của Supabase; tải một file qua URL do server cấp thì được phép (van-de V7, đã đóng).
- **Đã loại:** server proxy ảnh (tốn RAM/băng thông Render, ảnh không tải được khi server ngủ); Image Transformations của Supabase (giả định không có ở gói free); bucket public (ảnh nhạy cảm lộ).

### ADR-07. WebSocket chỉ là tín hiệu; ghi qua REST; một kênh `/user/queue/events`

**Đề xuất.** FR-CLAIM-03.

- **Quyết định:** STOMP (đã chốt). Client chỉ subscribe `/user/queue/events`; server gửi bằng `convertAndSendToUser`. Gửi tin nhắn bằng `POST /claims/{id}/messages`. App nhận sự kiện → `invalidateQueries` → tải lại qua REST.
- **Hệ quả:** không có kiểm tra quyền ở tầng subscribe (nơi dễ sót nhất); mất WS (Render ngủ, mạng yếu) chỉ làm chậm, không làm sai; test quyền chat là test REST bình thường.
- **Đã loại:** kênh theo claim `/topic/claims/{id}` (phải viết interceptor kiểm tra quyền subscribe); gửi tin nhắn qua STOMP SEND (hai đường ghi, hai chỗ kiểm tra quyền).

### ADR-08. Thông báo: ghi hộp thư trước, đẩy sau commit

**Đề xuất.** FR-NOTI-01..03.

- **Quyết định:** mọi thông báo là một dòng `notifications` ghi trong cùng transaction với sự kiện gây ra nó. Sau commit: người nhận có phiên WS → gửi sự kiện; không có → Expo Push (HTTP tới `exp.host`) nếu loại đó đang bật trong `notification_prefs`. Không kiểm tra receipt.
- **Hệ quả:** không bao giờ có push cho việc đã rollback; tắt push hệ thống vẫn còn hộp thư (FR-NOTI-02). Push thất bại im lặng (shortcut: không có receipt, nâng cấp nếu push hay mất).
- **Đã loại:** gọi FCM trực tiếp (thêm service account ở server, Expo Push đã chốt).

### ADR-09. Render free (Q7)

**Đề xuất, chốt sau đo đạc U4/U5.**

- **Quyết định:** JVM và Spring cấu hình ở 7.5; app có màn chờ đánh thức + retry chỉ cho lỗi mạng/5xx; GitHub Actions gọi `/health?db=true` **1 lần/ngày**; dự phòng demo bằng server trên laptop + màn đổi URL ẩn.
- **Thử nghiệm (việc S07 trong `docs/tien-do.md`, ngày 1–2):** `docker run -m 512m --cpus=0.1` trên máy mình để mô phỏng, đo thời gian khởi động và RSS; deploy lên Render, để ngủ 20 phút, đo `curl -w '%{time_total}' /api/v1/health` 3 lần. Đạt nếu: không OOM khi chạy 6 request đồng thời có upload ảnh, và thức < 90 s.
- **Nếu không đạt:** bỏ actuator, bật lazy init, thử CDS; vẫn không đạt thì báo nhóm (không có đường thoát miễn phí rõ ràng nào khác cho Spring, xem van-de V12).
- **Đã loại:** cron ping mỗi 10 phút để giữ thức (Render phản đối, BRD R9); GraalVM native image (build lâu, khó với nhóm, dễ vỡ với reflection).

### ADR-10. Schema `refound` riêng; kết nối qua Supavisor session pooler

**Đề xuất.** NFR-02, R10.

- **Quyết định:** bảng nằm ở schema `refound`, không có trong danh sách "exposed schemas" của Data API, nên dù ai có anon key của project cũng không đọc được qua PostgREST. Server nối bằng chuỗi **session pooler** (IPv4) vì kết nối trực tiếp của Supabase có thể chỉ có IPv6 mà Render free không hỗ trợ _(U7)_. Hikari tối đa 3 kết nối.
- **Hệ quả:** không cần RLS; không lộ bảng nếu cấu hình Supabase mặc định thay đổi.
- **Đã loại:** để ở `public` + bật RLS không policy (dễ quên với bảng mới); transaction pooler cổng 6543 (phải tắt prepared statement của JDBC).

### ADR-11. Đơn giản hóa máy trạng thái

**Đề xuất.** Mục 9 BRD.

- **Quyết định:** bỏ `draft` và `matched` khỏi trạng thái tin; gộp `handover_pending` vào `accepted`. Chi tiết và lý do ở đầu `05-may-trang-thai.md`.
- **Hệ quả:** ít trạng thái hơn, ít chuyển trạng thái ngược hơn (mã QR hết hạn không cần quay về `accepted`); mọi yêu cầu nghiệp vụ vẫn được đáp ứng.

### ADR-12. Không chia việc cố định; nhận việc theo `docs/tien-do.md` (Q6)

**Chốt ngày 22/11/2026 (nhóm quyết).**

- **Quyết định:** không gán module cố định cho từng người. Mọi việc nằm trong `docs/tien-do.md` với mã việc, điều kiện bắt đầu, file sẽ đụng và kết quả cần có. Ai rảnh thì nhận việc đầu tiên chưa bị chặn, ghi tên + nhánh vào file đó.
- **Hệ quả:** người hoặc AI mở phiên mới chỉ cần đọc một file là biết đang ở đâu và làm gì tiếp. Đổi lại phải giữ kỷ luật cập nhật file (quy tắc ở đầu `tien-do.md`). Để ít đụng file nhau: mỗi việc ghi rõ file sẽ sửa; hai việc đang làm không được cùng sửa một file ngoài danh sách "file dùng chung" (10.1).

### ADR-13. Tên migration theo thời gian + `outOfOrder`

**Đề xuất.**

- **Quyết định:** `V<yyyyMMddHHmm>__<module>_<mô tả>.sql`; `spring.flyway.out-of-order=true`; mọi PR có migration phải được một người khác đọc kỹ SQL; không sửa migration đã gộp `main` (AGENTS.md).
- **Hệ quả:** nhiều người tạo migration song song không tranh số V7/V8. `outOfOrder` cho phép gộp một migration có giờ cũ hơn migration đã chạy; vì vậy migration không được phụ thuộc thứ tự ngầm (chỉ phụ thuộc bảng P0).

### ADR-14. Giới hạn ảnh (Q5)

**Đề xuất.**

- **Quyết định:** tối đa 4 ảnh/tin (`found` ≥ 1, `lost` 0–4, `lost` nhạy cảm 0). Ảnh lớn cạnh dài 1024px, ≤ 1 MB (thường ~150–300 KB); ảnh nhỏ 512px (~30–60 KB). Ảnh đại diện 256px.
- **Hệ quả:** trường hợp xấu nhất ~1,2 MB/tin → 1 GB chứa ~800 tin, gấp đôi quy mô dự kiến (7.1).

### ADR-15. Q1: giả định đề tài chưa được duyệt

**Giả định.** Thiết kế không phụ thuộc kết quả duyệt; nhóm trưởng xin duyệt trước khi bắt đầu P1 (BRD R8). Nếu giảng viên đòi đổi phạm vi, P1/P2 là phần dễ điều chỉnh.

### ADR-16. Công thức chấm điểm và máy trạng thái ở server (Java), không ở app

**Đề xuất.**

- **Quyết định:** `MatchScorer`, `*StateMachine` viết bằng Java trong `domain`, test JUnit. App chỉ hiển thị.
- **Hệ quả:** một nguồn sự thật; không lộ trọng số và ngưỡng cho người đọc APK. `AGENTS.md` và NFR-09 đã sửa cho khớp (van-de V1, V2).

### ADR-17. Đáp án câu hỏi kín lưu dạng chữ, không băm

**Đề xuất.** BR-02, BR-03.

- **Bối cảnh:** Finder phải **đọc** được đáp án để tự so với câu trả lời của Owner (hệ thống không tự so khớp).
- **Quyết định:** lưu chữ thường trong bảng riêng `verification_secrets`; chỉ `GET /items/{id}/secret` (tác giả) trả về.
- **Hệ quả:** ai đọc được DB thì đọc được đáp án; chấp nhận vì chỉ server có quyền DB. Băm sẽ làm Finder không xem được.

### ADR-18. Chống trùng bằng luật, không bằng AI

**Đề xuất.** FR-MATCH-04. Lý do ở 4.5: trùng thực tế là cùng người đăng lại; luật đồng bộ, không tốn token, cảnh báo được **trước khi lưu** đúng như AC (embedding chạy nền nên không kịp).
