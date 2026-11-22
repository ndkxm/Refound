# Ví Lười

App quản lý chi tiêu cho người lười nhập: ghi bằng giọng nói, ảnh chuyển khoản, và trả lời mỗi ngày câu hỏi _"Hôm nay mình còn được tiêu bao nhiêu?"_. Đồ án môn IE307.

- Kế hoạch: [docs/ke-hoach.md](docs/ke-hoach.md)
- Quy ước code và quy tắc dùng AI: [AGENTS.md](AGENTS.md)
- Thiết lập Supabase / Firebase lần đầu: [docs/setup.md](docs/setup.md)

## Yêu cầu

- Node.js 22.13+ hoặc 24.3+
- Máy Android thật (khuyến nghị) hoặc Android Emulator
- Tài khoản Expo (để build development build bằng EAS)

## Chạy dự án

```bash
git clone https://github.com/ndkxm/ViLuoi.git
cd ViLuoi
npm install
cp .env.example .env   # rồi điền EXPO_PUBLIC_SUPABASE_URL và EXPO_PUBLIC_SUPABASE_ANON_KEY
npm start
```

App chạy bằng **development build** (không dùng Expo Go), nên cần cài build lên máy trước khi `npm start`.

### Cài development build lên máy Android

1. Mở link build TV1 gửi trong nhóm chat (trang build trên expo.dev) bằng điện thoại, hoặc quét mã QR trên trang đó, rồi bấm **Install** để tải file APK.
2. Cài APK. Android hỏi thì cho phép trình duyệt "cài ứng dụng không rõ nguồn gốc".
3. Trên máy tính: `npm start`. Điện thoại và máy tính phải chung Wi-Fi.
4. Mở app **Ví Lười** vừa cài, chọn server đang chạy (hoặc quét mã QR trong terminal). Sửa code là app tự tải lại.

Khác mạng hoặc Wi-Fi chặn kết nối giữa các máy (Wi-Fi trường): chạy `npx expo start --tunnel`.

Chỉ cần cài lại build khi nhóm thêm thư viện native hoặc đổi `app.config.ts`; TV1 build bằng `npx eas-cli@latest build --profile development --platform android` rồi gửi link mới. Sửa code JS/TS thì không cần.

## Lệnh thường dùng

| Lệnh                     | Việc                                                |
| ------------------------ | --------------------------------------------------- |
| `npm start`              | Chạy dev server                                     |
| `npm run lint`           | ESLint                                              |
| `npm run typecheck`      | Kiểm tra kiểu TypeScript                            |
| `npm test`               | Jest                                                |
| `npm run check`          | Chạy cả ba lệnh trên, bắt buộc trước khi mở PR      |
| `npm run format`         | Prettier định dạng lại code                         |
| `npx expo install <gói>` | Cài thư viện đúng phiên bản với Expo SDK            |
| `npx supabase <lệnh>`    | Supabase CLI (migration, Edge Functions, sinh kiểu) |

## Cấu trúc thư mục

```
src/
├── app/            # Màn hình (Expo Router): mỗi file là một route
├── features/       # Mỗi tính năng một thư mục: api / hooks / components / utils
│   ├── auth/  transactions/  capture/  budget/
│   └── location/  groups/  vietqr/  notifications/
├── components/ui/  # Component dùng chung (design system)
├── lib/            # Supabase client, query client, cấu hình
├── stores/         # Zustand
├── types/          # Kiểu sinh từ Supabase
├── utils/          # Hàm thuần: định dạng tiền, ngày, chuỗi tiếng Việt
└── global.css      # Tailwind (NativeWind)
supabase/
├── migrations/     # Schema, RLS, hàm SQL (chỉ sửa bằng file)
├── functions/      # Edge Functions (Deno)
└── seed.sql
docs/               # Kế hoạch và tài liệu
```
