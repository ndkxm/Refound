# AGENTS.md

Quy ước chung cho cả nhóm và cho mọi công cụ AI (Claude Code, Cursor, Copilot…). Đọc hết file này trước khi viết code.

## Dự án

**Ví Lười**: app quản lý chi tiêu cá nhân và chi tiêu nhóm, ghi bằng giọng nói / ảnh chuyển khoản, hiển thị con số "Hôm nay còn được tiêu". Đồ án 3 tuần, 6 người, Android là nền tảng chính.

- Kế hoạch đầy đủ: `docs/ke-hoach.md`. Chỉ đọc mục liên quan đến việc đang làm, không đọc cả file.
- Mỗi việc có mã `V01`–`V88` trong sheet phân công. Cột "Kết quả cần có" của việc là tiêu chí xong.
- Phạm vi 3 tuần: mục 4.7 của kế hoạch. Không làm tính năng nằm ngoài phạm vi khi chưa hỏi nhóm.

## Stack

| Lớp            | Dùng                                                                     |
| -------------- | ------------------------------------------------------------------------ |
| App            | Expo SDK 58, React Native 0.88, React 19, TypeScript 6 (strict)          |
| Điều hướng     | Expo Router, route nằm trong `src/app/`                                  |
| Giao diện      | NativeWind 4 (Tailwind 3) qua `className`                                |
| Dữ liệu server | TanStack Query                                                           |
| State cục bộ   | Zustand                                                                  |
| Form           | react-hook-form + zod                                                    |
| Backend        | Supabase: Postgres + RLS, Auth, Storage, Realtime, Edge Functions (Deno) |
| AI             | Gemini, chỉ gọi từ Edge Function                                         |
| Test           | Jest (`jest-expo`)                                                       |

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
npx supabase migration new <ten>
npx supabase gen types typescript --linked > src/types/database.ts
```

## Cấu trúc và người phụ trách

| Thư mục                       | Nội dung                                                                  | Phụ trách                               |
| ----------------------------- | ------------------------------------------------------------------------- | --------------------------------------- |
| `src/app/`                    | Màn hình (mỗi file là một route); chỉ ghép UI, không chứa logic nghiệp vụ | Người làm màn đó; khung điều hướng: TV1 |
| `src/features/auth/`          | Đăng nhập, onboarding                                                     | TV1                                     |
| `src/features/transactions/`  | Ví, danh mục, giao dịch, lịch sử                                          | TV2                                     |
| `src/features/capture/`       | Giọng nói, câu gõ, ảnh, màn xác nhận                                      | TV3 (giọng nói, câu gõ), TV4 (ảnh)      |
| `src/features/budget/`        | Safe-to-spend, ngân sách, khoản định kỳ                                   | TV5                                     |
| `src/features/location/`      | Vị trí, bản đồ, geofence                                                  | TV4                                     |
| `src/features/groups/`        | Nhóm, chia bill, sổ nợ                                                    | TV6                                     |
| `src/features/vietqr/`        | Sinh mã VietQR                                                            | TV6                                     |
| `src/features/notifications/` | Push, thông báo cục bộ                                                    | TV1                                     |
| `src/components/ui/`          | Design system dùng chung                                                  | TV1                                     |
| `src/lib/`, `src/stores/`     | Supabase client, query client, store dùng chung                           | TV1, TV2                                |
| `supabase/migrations/`        | Schema, RLS, hàm SQL                                                      | TV2 duyệt mọi thay đổi                  |
| `supabase/functions/`         | Edge Functions                                                            | Người làm function đó                   |

Mỗi feature chia nhỏ theo: `api/` (gọi Supabase), `hooks/` (TanStack Query), `components/`, `utils/` (hàm thuần, có test).

**Không sửa code trong thư mục của người khác** mà chưa báo người đó. Cần thay đổi thì nhắn hoặc mở PR nhỏ và tag họ.

## Quy ước code

- TypeScript strict, không dùng `any`. Kiểu bảng lấy từ `src/types/database.ts` (sinh tự động, không sửa tay).
- Import qua alias `@/` (trỏ vào `src/`), ví dụ `import { formatVnd } from '@/utils/money'`.
- Tên file viết thường, nối bằng gạch ngang: `amount-input.tsx`, `use-safe-to-spend.ts`.
- Giao diện viết bằng `className` của NativeWind. Màu, cỡ chữ, khoảng cách lấy từ `tailwind.config.js` (design system), không viết mã màu lẻ trong component.
- Dùng lại component trong `src/components/ui/` trước khi tạo cái mới.
- Mọi màn có đủ ba trạng thái: đang tải, lỗi (có nút thử lại), rỗng.
- Chữ hiển thị cho người dùng viết bằng tiếng Việt có dấu.

### Tiền và ngày

- **Tiền là số nguyên VNĐ**: `bigint` trong Postgres, `number` trong app. Không dùng số thực, không lưu dạng chuỗi. `amount` luôn dương; chiều tiền thể hiện bằng `type`.
- Hiển thị tiền bằng `formatVnd` trong `src/utils/money.ts`.
- Thời điểm lưu `timestamptz`. Tính "hôm nay", "chu kỳ" theo múi giờ `Asia/Ho_Chi_Minh`.

### Test

- Hàm thuần (tính tiền, safe-to-spend, rút gọn nợ, VietQR, chuẩn hóa chuỗi) phải có unit test, đặt cạnh file: `money.ts` → `money.test.ts`.
- Công thức và thuật toán: **tự tính tay ví dụ trước**, dùng làm đáp án trong test, rồi mới viết code.

## Supabase

- Mọi thay đổi schema là một file mới trong `supabase/migrations/`, không sửa tay trên dashboard. **Không sửa migration đã được gộp vào `main`**, chỉ thêm file mới.
- Bảng nào cũng bật RLS. Bảng cá nhân: `user_id = auth.uid()`. Bảng nhóm: chỉ thành viên nhóm. Thành viên không đọc được thu nhập của nhau (mục 7.3 kế hoạch).
- Mỗi migration có RLS phải kèm test truy cập chéo (user A không đọc được dữ liệu user B), do người viết tự viết, không giao cho AI tự kiểm.
- Sau khi đổi schema: sinh lại `src/types/database.ts` và commit cùng PR.
- Loại giao dịch và cách chi nhóm sinh giao dịch cá nhân: mục 6 "Chi tiêu nhóm và chi tiêu cá nhân" của kế hoạch. Không tự chế loại giao dịch mới.
- Edge Functions viết bằng TypeScript cho Deno. Key AI chỉ đọc từ secrets (`Deno.env.get('GEMINI_API_KEY')`).

## Bảo mật

- Không commit `.env`, `supabase/functions/.env`, key, mật khẩu, file `google-services.json`.
- Chỉ biến `EXPO_PUBLIC_*` vào được app, và ai có APK cũng đọc được. App chỉ được biết URL Supabase và anon key.
- **Không bao giờ** dùng service role key hay key AI trong code của app.
- Không dán `.env`, key, mật khẩu, ảnh chuyển khoản chưa che vào chat với AI.

## Git

- Nhánh: `feature/<mã việc>-<ten-ngan>`, ví dụ `feature/V39-parse-voice`. Tách từ `main`, gộp lại `main` qua PR.
- Commit theo Conventional Commits, ghi mã việc: `feat(capture): parse-voice edge function (V39)`.
- PR nhỏ (dưới khoảng 400 dòng, không tính file sinh tự động), một việc một PR, cần ít nhất 1 người duyệt.
- Trước khi mở PR: `npm run check` qua hết. Màn mới thì đính kèm ảnh chụp trên máy Android.
- Thêm thư viện mới phải hỏi TV1 trước: thư viện native bắt cả nhóm build lại development build.

## Quy tắc khi dùng AI

1. Dán kèm đoạn kế hoạch liên quan (bảng dữ liệu, công thức, luồng) và cột "Kết quả cần có" của việc vào prompt.
2. Người mở PR phải hiểu và giải thích được mọi dòng trong PR, kể cả phần AI viết. Khi bảo vệ, giảng viên hỏi người làm.
3. AI không được tự ý: sửa migration đã gộp, thêm thư viện, đổi cấu trúc thư mục, sửa file ngoài phạm vi việc đang làm.
4. Kẹt quá 30 phút với AI ở cùng một lỗi thì hỏi người trong nhóm và đổi trạng thái việc sang "Bị chặn".
5. Ghi cách dùng AI hiệu quả vào cột Ghi chú của việc; khai báo việc dùng AI trong báo cáo theo quy định của môn.

## Một việc được coi là xong khi

- [ ] Chạy được trên máy Android thật
- [ ] Có xử lý đang tải, lỗi, rỗng (nếu là màn hình)
- [ ] Logic nghiệp vụ có unit test, `npm run check` qua hết
- [ ] PR đã được duyệt và gộp vào `main`
- [ ] Người làm giải thích được code
- [ ] Đã đổi trạng thái trong sheet sang "Xong"
