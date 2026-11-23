# 11. Bản ghi quyết định (ADR)

Mỗi ADR: bối cảnh → quyết định → hệ quả → phương án đã loại. Trạng thái "Đề xuất" nghĩa là nhóm chưa xác nhận. Ngày 22–23/11/2026 các ADR còn "Đề xuất" đã được chốt (việc S01, D5), kể cả ADR-09 (Kiên duyệt không chờ số đo Render).

---

### ADR-01. Đối chiếu nền chạy trong tiến trình Spring, hàng đợi là cột của `items`

**Chốt ngày 22/11/2026** FR-MATCH-03, NFR-05.

- **Bối cảnh:** cần chạy embedding + đối chiếu sau khi đăng, chịu được lỗi AI và việc Render tắt server bất cứ lúc nào. Một instance, vài trăm tin.
- **Quyết định:** `embedding_status / embedding_attempts / next_embed_at` trên `items` là hàng đợi. Job chạy ngay sau commit (`@TransactionalEventListener` + executor 1 luồng) và `@Scheduled` mỗi 30 s vét lại; lấy việc bằng `FOR UPDATE SKIP LOCKED`.
- **Hệ quả:** không thêm dịch vụ; trạng thái job nằm trong DB nên server chết không mất việc; giải thích được bằng một câu SQL. Server ngủ thì job dừng, nhưng việc chỉ sinh ra khi có request, mà request đánh thức server.
- **Đã loại:** trigger Postgres (không gọi HTTP ra ngoài được một cách an toàn), hàng đợi ngoài (Redis/RabbitMQ: thêm dịch vụ, không free lâu dài), bảng `jobs` tổng quát (chỉ có một loại việc).

### ADR-02. Embedding bằng Gemini `gemini-embedding-2` 768 chiều; mô tả ảnh bằng Groq `qwen/qwen3.8-27b` (Q3)

**Chốt ngày 23/11/2026 (Kiên quyết, D5 + D11).** Thay bản đề xuất đầu (`jina-clip-v2`, dự phòng Groq mô tả ảnh + `gemini-embedding-001`).

- **Bối cảnh:** cần so chữ↔ảnh (tin mất thường chỉ có chữ); số chiều phải chốt trước migration. Mô tả của sinh viên thường ngắn ("tai nghe đen"), câu tìm kiếm tiếng Việt so với chữ thường sát hơn so với ảnh.
- **Quyết định:** chỉ gọi **Gemini API** từ server, model `gemini-embedding-2` với `output_dimensionality=768` cho cả chữ và ảnh (chung một không gian vector). Chữ của tin dùng tiền tố `title: {danh mục} | text: {mô tả}`, câu tìm kiếm dùng `task: search result | query: {chữ}` (model không nhận `task_type`). Code gọi qua interface `EmbeddingClient` (để thay bằng bản giả trong test); cột `model` ghi model đã dùng.
  - **Mô tả ảnh:** trước khi embed, job gửi tối đa 3 ảnh nhỏ 512px của tin (không gửi ảnh danh mục nhạy cảm, BR-08) cho **Groq** model `qwen/qwen3.8-27b` (`reasoning_effort="none"`, `response_format={"type":"json_object"}`), nhận `{"mo_ta": "…"}`: một câu tiếng Việt ≤ 40 từ về loại đồ, màu, hãng, đặc điểm nhận dạng, không chép chữ hay số trên đồ vật. Lưu vào `items.ai_caption`, ghép vào chữ: `title: {danh mục} | text: {mô tả}. {ai_caption}`. Chỉ dùng để embed, **không trả về app** (tránh hiện mô tả sai). Gọi qua interface `VisionClient`; tên model đọc từ `GROQ_VISION_MODEL`.
  - Groq lỗi, 429 hay hết hạn mức → bỏ mô tả ảnh, embed tiếp ngay (không tính vào `embedding_attempts`, không thử lại mô tả). Vector ảnh của Gemini vẫn có nên đối chiếu không mất chức năng.
- **Hệ quả:**
  - Hai key (`GEMINI_API_KEY`, `GROQ_API_KEY`), không tốn tiền ở gói free. Không làm thử nghiệm trước (S08 bỏ ngày 23/11/2026); ngưỡng `c0`, `c1` lấy từ bộ mẫu ở P0-13.
  - `qwen/qwen3.8-27b` đang **preview** trên Groq (có thể bị gỡ khi báo trước ngắn), tối đa 3 ảnh/request, hạn mức ghi trong tài liệu 30 request/phút, 1.000 request/ngày _(U8)_. Mỗi tin 1 lời gọi; bị gỡ thì đổi `GROQ_VISION_MODEL` sang model vision khác, hoặc để trống để tắt bước mô tả.
  - Giới hạn là số request theo phút/ngày, tính **theo project Google Cloud** chứ không theo key _(U2)_: cả nhóm dùng chung một project thì chung hạn mức.
  - Ở gói free, nội dung gửi lên (mô tả, ảnh nhỏ 512px) **được Google dùng để cải thiện sản phẩm**. Chấp nhận vì ảnh là đồ vật; ảnh danh mục nhạy cảm vốn không gửi (BR-08). Không gửi tên người, số thẻ hay vị trí.
  - Không gian vector của `gemini-embedding-2` không tương thích với model khác (kể cả `gemini-embedding-001`): đổi model phải embed lại toàn bộ.
- **Đã loại:** `jina-clip-v2` (thêm nhà cung cấp, tính token theo ảnh, số dư có thể hết); chỉ dùng mô tả ảnh của Groq, bỏ vector ảnh (mất thông tin hình ảnh khi chuyển thành chữ); hiện mô tả của AI cho người dùng hoặc tự điền form (tính năng mới ngoài đề cương); `gemini-embedding-001` (chỉ nhận chữ); số chiều 3072 mặc định (gấp 4 dung lượng, không cần ở vài trăm tin).

### ADR-03. Không tạo chỉ mục vector (HNSW/IVFFlat)

**Chốt ngày 22/11/2026**

- **Bối cảnh:** vài trăm tin × ~3 vector. Truy vấn luôn kèm bộ lọc (loại, trạng thái, khoảng cách, thời gian).
- **Quyết định:** quét tuần tự, tính cosine chính xác.
- **Hệ quả:** kết quả chính xác 100%, không có lỗi "lọc sau HNSW trả thiếu kết quả"; vẫn nhanh (ước lượng < 50 ms). Khi > ~10.000 vector thì thêm `CREATE INDEX ... USING hnsw (embedding vector_cosine_ops)` bằng migration mới.
- **Đã loại:** HNSW ngay từ đầu (gần đúng, thêm tham số phải giải thích, không cần ở quy mô này).

### ADR-04. Phân quyền: kiểm tra sở hữu ở service bằng hàm thuần `*Access`

**Chốt ngày 22/11/2026** NFR-01, R10.

- **Quyết định:** Spring Security lo xác thực JWT và vai trò admin theo URL (+ `@PreAuthorize("hasRole('ADMIN')")` trên controller admin). Mọi kiểm tra "bản ghi này có phải của tôi / tôi có phải một bên của claim" nằm ở `XxxAccess.requireYyy(record, me)` gọi đầu mỗi method service. DTO tách theo người xem.
- **Hệ quả:** đọc một method service là thấy đủ quyền; test hàm thuần không cần Spring; test truy cập chéo vẫn bắt buộc vì quên gọi `require` là lỗi phổ biến nhất.
- **Đã loại:** SpEL `@PreAuthorize("@claimSecurity.isParticipant(#id, principal)")` (khó đọc, khó test, phải truy DB hai lần); RLS Postgres với `SET LOCAL` user id (hai nơi định nghĩa quyền, khó debug qua pooler).

### ADR-05. Đăng nhập bằng Google (Firebase Auth); server tự phát hành JWT

**Chốt ngày 22/11/2026 (nhóm quyết).** Thay cho "email bất kỳ, không xác minh" ở FR-AUTH-01 và BRD 5.2. **Sửa ngày 22/11/2026 (D10, Kiên quyết): bỏ đăng nhập số điện thoại + OTP, chỉ còn Google; vẫn đi qua Firebase Auth.** BRD sửa theo (v0.3).

- **Bối cảnh:** đăng ký bằng email không xác minh thì tạo tài khoản giả quá dễ (van-de V5) và quên mật khẩu là mất tài khoản (V6). Bản đầu cho chọn Google hoặc SMS OTP. SMS bị bỏ: có thể tốn tiền (Firebase có thể đòi gói Blaze gắn thẻ, trái G3), cần thêm màn nhập số, màn OTP, số thử và test chuẩn hóa số, trong khi sinh viên đều có tài khoản Google.
- **Quyết định:**
  1. App dùng `@react-native-google-signin/google-signin` lấy credential Google → **Firebase Auth** `signInWithCredential`. Project Firebase đã có sẵn vì dùng cho push (FCM). Giữ Firebase (thay vì server kiểm tra thẳng Google ID token) để đường xác minh ở server không đổi nếu sau này thêm cách đăng nhập khác.
  2. App gửi **Firebase ID token** lên `POST /auth/firebase`. Server kiểm tra token bằng `NimbusJwtDecoder.withJwkSetUri(<khóa công khai của Google cho securetoken>)` có sẵn trong Spring Security: chữ ký RS256, `iss = https://securetoken.google.com/<projectId>`, `aud = <projectId>`, còn hạn, `firebase.sign_in_provider = google.com` _(U18)_. Không cần Firebase Admin SDK.
  3. Server tìm hoặc tạo `users` theo `firebase_uid` rồi phát hành **JWT của riêng mình** (HS256, `JWT_SECRET`, `sub=userId`, `role`, hạn 7 ngày). Mọi API khác chỉ dùng JWT này. Mỗi request kiểm tra `users.status='active'` (một truy vấn theo PK).
  4. Server **không lưu** email hay mật khẩu; chỉ lưu `firebase_uid`. Tên hiển thị do người dùng nhập (gợi ý sẵn tên Google).
- **Hệ quả:** không có bảng mật khẩu, không có luồng quên mật khẩu, không phải chống dò mật khẩu. Tạo tài khoản giả khó hơn email (cần một tài khoản Google) nhưng vẫn làm được (van-de V5). Thêm 3 thư viện native (8.1) và bước khai báo SHA-1/SHA-256 của khóa ký APK trong Firebase. Không tốn tiền. Người không có tài khoản Google không dùng được app (chấp nhận, đối tượng là sinh viên).
- **Đã loại:** SMS OTP qua Firebase (có thể tốn tiền, thêm màn và test; bỏ ngày 22/11/2026); tự gửi SMS qua nhà mạng/Twilio (tốn tiền); Supabase Auth (app phải gọi thẳng Supabase); Google OAuth qua `expo-auth-session` + SMS qua Firebase (hai đường xác minh ở server thay vì một); refresh token (thêm bảng và luồng không cần cho 6–10 người).

### ADR-06. Ảnh: resize ở app, upload qua server, đọc bằng signed URL

**Chốt ngày 22/11/2026** Q5, BR-08, NFR-02.

- **Quyết định:** app tạo 2 bản JPEG (lớn 1024px ~ chất lượng 0,7; nhỏ 512px ~ 0,6) và gửi multipart lên server; server kiểm tra (JPEG, ≤ 1 MB, ≤ 4 ảnh) rồi upload Storage bằng service key. Đọc ảnh: server trả signed URL hết hạn 1 giờ (ký theo lô một lần gọi); app tải ảnh trực tiếp từ URL đó. Ảnh nhạy cảm chỉ được ký cho người có quyền (3.2).
- **Hệ quả:** RAM server không phải giải mã ảnh; ảnh không đi qua Render khi đọc (đỡ băng thông, đỡ chờ server thức). URL ký không chứa khóa, chỉ mở được đúng một file trong 1 giờ, và chỉ server quyết ai được cấp. Vì vậy nguyên tắc "app không gọi thẳng Supabase" được hiểu là: app không giữ khóa, không gọi API dữ liệu/Storage của Supabase; tải một file qua URL do server cấp thì được phép (van-de V7, đã đóng).
- **Đã loại:** server proxy ảnh (tốn RAM/băng thông Render, ảnh không tải được khi server ngủ); Image Transformations của Supabase (giả định không có ở gói free); bucket public (ảnh nhạy cảm lộ).

### ADR-07. WebSocket chỉ là tín hiệu; ghi qua REST; một kênh `/user/queue/events`

**Chốt ngày 22/11/2026** FR-CLAIM-03.

- **Quyết định:** STOMP (đã chốt). Client chỉ subscribe `/user/queue/events`; server gửi bằng `convertAndSendToUser`. Gửi tin nhắn bằng `POST /claims/{id}/messages`. App nhận sự kiện → `invalidateQueries` → tải lại qua REST.
- **Hệ quả:** không có kiểm tra quyền ở tầng subscribe (nơi dễ sót nhất); mất WS (Render ngủ, mạng yếu) chỉ làm chậm, không làm sai; test quyền chat là test REST bình thường.
- **Đã loại:** kênh theo claim `/topic/claims/{id}` (phải viết interceptor kiểm tra quyền subscribe); gửi tin nhắn qua STOMP SEND (hai đường ghi, hai chỗ kiểm tra quyền).

### ADR-08. Thông báo: ghi hộp thư trước, đẩy sau commit

**Chốt ngày 22/11/2026** FR-NOTI-01..03.

- **Quyết định:** mọi thông báo là một dòng `notifications` ghi trong cùng transaction với sự kiện gây ra nó. Sau commit: luôn gửi sự kiện WS (nếu đang nối) **và** Expo Push (HTTP tới `exp.host`) nếu loại đó đang bật trong `notification_prefs`. App đang mở thì tự ẩn banner bằng handler của `expo-notifications` (chỉ cập nhật hộp thư). Không kiểm tra receipt.
- **Hệ quả:** không bao giờ có push cho việc đã rollback; tắt push hệ thống vẫn còn hộp thư (FR-NOTI-02). Push thất bại im lặng (shortcut: không có receipt, nâng cấp nếu push hay mất).
- **Đã loại:** gọi FCM trực tiếp (thêm service account ở server, Expo Push đã chốt); chỉ push khi người nhận không có phiên WS (bản đề xuất đầu: trên Android, app vừa chuyển nền vẫn giữ socket vài phút nên người dùng mất thông báo; server còn phải tra `SimpUserRegistry`).

### ADR-09. Render free (Q7)

**Chốt ngày 23/11/2026 (Kiên duyệt, không chờ đo trên Render).** Số đo local (S07): thức sau 77 s, RSS 120 MiB trong giới hạn 512 MB. Khi deploy vẫn đo theo mục Thử nghiệm; không đạt thì làm mục "Nếu không đạt".

- **Quyết định:** JVM và Spring cấu hình ở 7.5; app có màn chờ đánh thức + retry chỉ cho lỗi mạng/5xx; GitHub Actions gọi `/health?db=true` **1 lần/ngày**; dự phòng demo bằng server trên laptop + màn đổi URL ẩn.
- **Thử nghiệm (việc S07 trong `docs/tien-do.md`, ngày 1–2):** `docker run -m 512m --cpus=0.1` trên máy mình để mô phỏng, đo thời gian khởi động và RSS; deploy lên Render, để ngủ 20 phút, đo `curl -w '%{time_total}' /api/v1/health` 3 lần. Đạt nếu: không OOM khi chạy 6 request đồng thời có upload ảnh, và thức < 90 s.
- **Nếu không đạt:** bỏ actuator, bật lazy init, thử CDS; vẫn không đạt thì báo nhóm (không có đường thoát miễn phí rõ ràng nào khác cho Spring, xem van-de V12).
- **Đã loại:** cron ping mỗi 10 phút để giữ thức (Render phản đối, BRD R9); GraalVM native image (build lâu, khó với nhóm, dễ vỡ với reflection).

### ADR-10. Schema `refound` riêng; kết nối qua Supavisor session pooler

**Chốt ngày 22/11/2026** NFR-02, R10.

- **Quyết định:** bảng nằm ở schema `refound`, không có trong danh sách "exposed schemas" của Data API, nên dù ai có anon key của project cũng không đọc được qua PostgREST. Server nối bằng chuỗi **session pooler** (IPv4) vì kết nối trực tiếp của Supabase có thể chỉ có IPv6 mà Render free không hỗ trợ _(U7)_. Hikari tối đa 3 kết nối.
- **Hệ quả:** không cần RLS; không lộ bảng nếu cấu hình Supabase mặc định thay đổi.
- **Đã loại:** để ở `public` + bật RLS không policy (dễ quên với bảng mới); transaction pooler cổng 6543 (phải tắt prepared statement của JDBC).

### ADR-11. Đơn giản hóa máy trạng thái

**Chốt ngày 22/11/2026** Mục 9 BRD.

- **Quyết định:** bỏ `draft` và `matched` khỏi trạng thái tin; gộp `handover_pending` vào `accepted`. Chi tiết và lý do ở đầu `05-may-trang-thai.md`.
- **Hệ quả:** ít trạng thái hơn, ít chuyển trạng thái ngược hơn (mã QR hết hạn không cần quay về `accepted`); mọi yêu cầu nghiệp vụ vẫn được đáp ứng.

### ADR-12. Không chia việc cố định; nhận việc theo `docs/tien-do.md` (Q6)

**Chốt ngày 22/11/2026 (nhóm quyết).**

- **Quyết định:** không gán module cố định cho từng người. Mọi việc nằm trong `docs/tien-do.md` với mã việc, điều kiện bắt đầu, file sẽ đụng và kết quả cần có. Ai rảnh thì nhận việc đầu tiên chưa bị chặn, ghi tên + nhánh vào file đó.
- **Hệ quả:** người hoặc AI mở phiên mới chỉ cần đọc một file là biết đang ở đâu và làm gì tiếp. Đổi lại phải giữ kỷ luật cập nhật file (quy tắc ở đầu `tien-do.md`). Để ít đụng file nhau: mỗi việc ghi rõ file sẽ sửa; hai việc đang làm không được cùng sửa một file ngoài danh sách "file dùng chung" (10.1).

### ADR-13. Tên migration theo thời gian + `outOfOrder`

**Chốt ngày 22/11/2026**

- **Quyết định:** `V<yyyyMMddHHmm>__<module>_<mô tả>.sql`; `spring.flyway.out-of-order=true`; mọi PR có migration phải được một người khác đọc kỹ SQL; không sửa migration đã gộp `main` (AGENTS.md).
- **Hệ quả:** nhiều người tạo migration song song không tranh số V7/V8. `outOfOrder` cho phép gộp một migration có giờ cũ hơn migration đã chạy; vì vậy migration không được phụ thuộc thứ tự ngầm (chỉ phụ thuộc bảng P0).

### ADR-14. Giới hạn ảnh (Q5)

**Chốt ngày 22/11/2026**

- **Quyết định:** tối đa 4 ảnh/tin (`found` ≥ 1, `lost` 0–4, `lost` nhạy cảm 0). Ảnh lớn cạnh dài 1024px, ≤ 1 MB (thường ~150–300 KB); ảnh nhỏ 512px (~30–60 KB). Ảnh đại diện 256px.
- **Hệ quả:** trường hợp xấu nhất ~1,2 MB/tin → 1 GB chứa ~800 tin, gấp đôi quy mô dự kiến (7.1).

### ADR-15. Q1: giả định đề tài chưa được duyệt

**Giả định.** Thiết kế không phụ thuộc kết quả duyệt; nhóm trưởng xin duyệt trước khi bắt đầu P1 (BRD R8). Nếu giảng viên đòi đổi phạm vi, P1/P2 là phần dễ điều chỉnh.

### ADR-16. Công thức chấm điểm và máy trạng thái ở server (Java), không ở app

**Chốt ngày 22/11/2026**

- **Quyết định:** `MatchScorer`, `*StateMachine` viết bằng Java trong `domain`, test JUnit. App chỉ hiển thị.
- **Hệ quả:** một nguồn sự thật; không lộ trọng số và ngưỡng cho người đọc APK. `AGENTS.md` và NFR-09 đã sửa cho khớp (van-de V1, V2).

### ADR-17. Đáp án câu hỏi kín lưu dạng chữ, không băm

**Chốt ngày 22/11/2026** BR-02, BR-03.

- **Bối cảnh:** Finder phải **đọc** được đáp án để tự so với câu trả lời của Owner (hệ thống không tự so khớp).
- **Quyết định:** lưu chữ thường trong bảng riêng `verification_secrets`; chỉ `GET /items/{id}/secret` (tác giả) trả về.
- **Hệ quả:** ai đọc được DB thì đọc được đáp án; chấp nhận vì chỉ server có quyền DB. Băm sẽ làm Finder không xem được.

### ADR-18. Chống trùng bằng luật, không bằng AI

**Chốt ngày 22/11/2026** FR-MATCH-04. Lý do ở 4.5: trùng thực tế là cùng người đăng lại; luật đồng bộ, không tốn token, cảnh báo được **trước khi lưu** đúng như AC (embedding chạy nền nên không kịp).
