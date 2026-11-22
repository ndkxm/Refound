# AGENTS.md

Quy ước chung cho cả nhóm và cho mọi công cụ AI (Claude Code, Cursor, Copilot…). Đọc hết file này trước khi viết code.

## Dự án

**Refound**: app tìm đồ thất lạc trong trường. Đăng đồ nhặt được / làm mất kèm ảnh và vị trí, hệ thống tự đối chiếu (vector search + vị trí + thời gian), xác minh chủ nhân trước khi bàn giao. Đồ án 3 tuần, 6 người, Android là nền tảng chính.

- Đề cương: `docs/ke-hoach.md`.
- **Tiến độ: `docs/tien-do.md`.** Đọc đầu tiên mỗi phiên để biết đã làm đến đâu và việc nào làm tiếp được; cập nhật theo hướng dẫn ở đầu file đó. Thiết kế: `docs/kien-truc/`.
- Không làm tính năng nằm ngoài phạm vi đề cương khi chưa hỏi nhóm.

## Stack

| Lớp            | Dùng                                                                                              |
| -------------- | ------------------------------------------------------------------------------------------------- |
| App            | Expo SDK 58, React Native 0.88, React 19, TypeScript 6 (strict)                                   |
| Điều hướng     | Expo Router, route nằm trong `src/app/`                                                           |
| Giao diện      | NativeWind 4 (Tailwind 3) qua `className`                                                         |
| Dữ liệu server | TanStack Query                                                                                    |
| State cục bộ   | Zustand                                                                                           |
| Form           | react-hook-form + zod                                                                             |
| Backend        | Spring Boot 3, Java 21, Maven, trong `server/` (REST + WebSocket)                                 |
| CSDL, ảnh      | Supabase: Postgres (pgvector, PostGIS) + Storage; migration bằng Flyway                           |
| AI             | Groq (LLM / vision) + Jina (embedding), chỉ gọi từ server Spring                                  |
| Đăng nhập      | Firebase Auth (Google hoặc số điện thoại + OTP); server kiểm tra ID token rồi phát hành JWT riêng |
| Triển khai     | Server chạy trên Render (đóng gói Docker); app phát hành bằng APK                                 |
| Test           | Jest (`jest-expo`) cho app, JUnit 5 cho server                                                    |

### Expo thay đổi nhiều qua từng SDK, đừng tin trí nhớ

Trước khi viết code đụng tới API của Expo / EAS / React Native:

1. Xem phiên bản `expo` trong `package.json` (hiện là 58).
2. Đọc tài liệu đúng phiên bản: `https://docs.expo.dev/versions/v58.0.0/`, hoặc mục lục `https://docs.expo.dev/llms.txt`.
3. Cài thư viện bằng `npx expo install <gói>`, không dùng `npm install` cho thư viện chạy trong app.

## Lệnh

```bash
npm start              # dev server
npm run check          # lint + typecheck + test, BẮT BUỘC chạy trước khi mở PR
npm run lint
npm run typecheck
npm test
npm run format         # Prettier
npx expo-doctor        # kiểm tra cấu hình và phiên bản thư viện

# Server (chạy trong thư mục server/)
./mvnw spring-boot:run   # chạy server, profile local
./mvnw test              # chỉ unit test (không cần Docker)
./mvnw verify            # unit + test tích hợp + test truy cập chéo (cần Docker), BẮT BUỘC qua trước khi mở PR có sửa server
```

## Cấu trúc

Nhóm **không chia thư mục cố định theo người**. Ai đang làm gì xem cột "File chính" của các việc đang làm trong `docs/tien-do.md`.

| Thư mục                                   | Nội dung                                                                                      |
| ----------------------------------------- | --------------------------------------------------------------------------------------------- |
| `src/app/`                                | Màn hình (mỗi file là một route); chỉ ghép UI, không chứa logic nghiệp vụ                     |
| `src/features/auth/`                      | Đăng nhập Google / số điện thoại, đặt tên hiển thị, hồ sơ                                     |
| `src/features/items/`                     | Đăng đồ mất / nhặt được, bản đồ, danh sách, chi tiết                                          |
| `src/features/matching/`                  | Màn tìm kiếm, danh sách món có thể khớp, báo khớp chủ động (chỉ hiển thị; chấm điểm ở server) |
| `src/features/claims/`                    | Xác minh chủ nhân, bàn giao bằng QR, chat ẩn danh                                             |
| `src/features/notifications/`             | Push, hộp thông báo, cài đặt thông báo                                                        |
| `src/components/ui/`                      | Design system dùng chung                                                                      |
| `src/lib/`, `src/stores/`                 | API client, query client, WebSocket client, store dùng chung                                  |
| `server/`                                 | Backend Spring Boot (Maven), chia module theo nghiệp vụ                                       |
| `server/src/main/resources/db/migration/` | Schema, ràng buộc, hàm SQL (Flyway); mọi PR có migration cần một người khác đọc kỹ SQL        |

Mỗi feature chia nhỏ theo: `api/` (gọi REST của server), `hooks/` (TanStack Query), `components/`, `utils/` (hàm thuần, có test).

**Không sửa file đang nằm trong cột "File chính" của một việc người khác đang làm** (🔵 trong `docs/tien-do.md`) mà chưa báo người đó. Cần thay đổi thì nhắn hoặc mở PR nhỏ và tag họ.

## Quy ước code

- TypeScript strict, không dùng `any`. Kiểu dữ liệu từ server khai báo trong `src/types/` (khớp hợp đồng API).
- Import qua alias `@/` (trỏ vào `src/`), ví dụ `import { cn } from '@/utils/cn'`.
- Tên file viết thường, nối bằng gạch ngang: `item-card.tsx`, `use-match-score.ts`.
- Giao diện viết bằng `className` của NativeWind. Màu, cỡ chữ, khoảng cách lấy từ `tailwind.config.js` (design system), không viết mã màu lẻ trong component.
- Dùng lại component trong `src/components/ui/` trước khi tạo cái mới.
- Mọi màn có đủ ba trạng thái: đang tải, lỗi (có nút thử lại), rỗng.
- Chữ hiển thị cho người dùng viết bằng tiếng Việt có dấu.

### Ngày giờ và vị trí

- Thời điểm lưu `timestamptz`, hiển thị theo múi giờ `Asia/Ho_Chi_Minh`.
- Vị trí lưu kiểu `geography(Point, 4326)` (PostGIS); khoảng cách tính bằng mét.

### Test

- Hàm thuần phải có unit test.
  - Server (JUnit, trong `server/src/test/java/` cùng package): điểm khớp, máy trạng thái tin và bàn giao, kiểm tra quyền (`*Access`), làm tròn vị trí.
  - App (Jest, đặt cạnh file: `format-distance.ts` → `format-distance.test.ts`): định dạng khoảng cách/thời gian, chuẩn hóa số điện thoại, schema form.
- Công thức và thuật toán: **tự tính tay ví dụ trước**, dùng làm đáp án trong test, rồi mới viết code.

## Server (Spring) và CSDL

- App chỉ nói chuyện với server qua REST/WebSocket. **App không gọi thẳng Supabase** và không giữ khóa Supabase nào.
- Cấu trúc module trong `server/`: `com.refound.<module>.{presentation, application, domain, infrastructure}` (đề xuất, theo cách tổ chức của backend cũ). Module: `auth`, `item`, `matching`, `claim`, `chat`, `notification`, `moderation` (báo cáo vi phạm, điểm uy tín, quản trị).
- Mọi thay đổi schema là một file migration Flyway mới, không sửa tay trên dashboard. **Không sửa migration đã được gộp vào `main`**, chỉ thêm file mới.
- Phân quyền nằm ở server (Spring Security + kiểm tra ở service), không còn RLS làm chốt chặn chính. Câu hỏi xác minh, đáp án, chat và thông tin liên lạc chỉ trả về cho đúng người liên quan.
- Mỗi endpoint có kiểm soát quyền phải kèm test truy cập chéo (user A gọi API lấy dữ liệu của user B phải bị từ chối), do người viết tự viết, không giao cho AI tự kiểm.
- Chuyển trạng thái tin và yêu cầu nhận đồ chỉ xảy ra ở server, trong một transaction.
- Key Groq, key Jina, khóa Supabase và JWT secret chỉ đọc từ biến môi trường của server (Render Environment), không commit.
- Server không lưu email, số điện thoại hay mật khẩu; người dùng định danh bằng `firebase_uid` lấy từ Firebase ID token đã kiểm tra chữ ký.
- Server chạy trên Render gói miễn phí: tự tắt sau khoảng 15 phút không có truy cập và khởi động lại chậm. Thiết kế app phải chịu được lần gọi đầu rất chậm (hiện trạng thái chờ, thử lại).

## Bảo mật

- Không commit `.env`, `server/**/application-local.yml` có chứa bí mật, key, mật khẩu, file `google-services.json`.
- Chỉ biến `EXPO_PUBLIC_*` vào được app, và ai có APK cũng đọc được. App chỉ được biết URL của server.
- **Không bao giờ** đặt khóa Supabase hay key AI trong code của app.
- Không dán `.env`, key, mật khẩu, ảnh chuyển khoản chưa che vào chat với AI.

## Git

- Nhánh: `feature/<ten-ngan>`, ví dụ `feature/embedding-service`. Tách từ `main`, gộp lại `main` qua PR.
  - Ngoại lệ duy nhất: commit chỉ sửa `docs/tien-do.md` để nhận việc hoặc báo bị chặn được đẩy thẳng lên `main` (message `docs(tien-do): nhận <mã việc>`). Cách làm ở đầu file đó.
- Commit theo Conventional Commits, ghi mã việc: `feat(matching): embedding service`.
- PR nhỏ (dưới khoảng 400 dòng, không tính file sinh tự động), một việc một PR, cần ít nhất 1 người duyệt.
- Trước khi mở PR: `npm run check` qua hết; có sửa server thì `./mvnw verify` cũng qua. Màn mới thì đính kèm ảnh chụp trên máy Android.
- Thêm thư viện mới phải hỏi nhóm trước: thư viện native bắt cả nhóm build lại development build. Thêm dependency vào `pom.xml` cũng báo nhóm.

## Quy tắc khi dùng AI

1. Cho AI đọc `docs/tien-do.md` và các mục ở cột "Thiết kế" của việc; dán kèm cột "Kết quả cần có" vào prompt.
2. Người mở PR phải hiểu và giải thích được mọi dòng trong PR, kể cả phần AI viết. Khi bảo vệ, giảng viên hỏi người làm.
3. AI không được tự ý: sửa migration đã gộp, thêm thư viện, đổi cấu trúc thư mục, sửa file ngoài phạm vi việc đang làm.
4. Kẹt quá 30 phút với AI ở cùng một lỗi thì hỏi người trong nhóm và đổi trạng thái việc sang 🔴 Bị chặn trong `docs/tien-do.md`.
5. Ghi cách dùng AI hiệu quả vào mục "Ghi chú theo việc" trong `docs/tien-do.md`; khai báo việc dùng AI trong báo cáo theo quy định của môn.

## Một việc được coi là xong khi

- [ ] Chạy được trên máy Android thật
- [ ] Có xử lý đang tải, lỗi, rỗng (nếu là màn hình)
- [ ] Logic nghiệp vụ có unit test, `npm run check` qua hết (và `./mvnw verify` nếu có sửa server)
- [ ] PR đã được duyệt và gộp vào `main`
- [ ] Người làm giải thích được code
- [ ] Đã đổi trạng thái việc sang ✅ trong `docs/tien-do.md`
