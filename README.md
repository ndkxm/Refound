# Refound

App tìm đồ thất lạc trong trường: đăng đồ nhặt được hoặc làm mất kèm ảnh và vị trí, tự đối chiếu bằng vector search, xác minh chủ nhân trước khi bàn giao. Đồ án môn IE307.

## Yêu cầu

- Node.js 22.13+ hoặc 24.3+
- JDK 21 và Maven (hoặc `./mvnw`) cho server
- Máy Android thật (khuyến nghị) hoặc Android Emulator
- Tài khoản Expo (để build development build bằng EAS)

## Chạy dự án

```bash
git clone <url-repo>
cd refound
npm install
cp .env.example .env   # rồi điền EXPO_PUBLIC_API_URL (địa chỉ server)
npm start
```

App chạy bằng **development build** (không dùng Expo Go), nên cần cài build lên máy trước khi `npm start`.

### Cài development build lên máy Android

1. Mở link build người phụ trách khung gửi trong nhóm chat (trang build trên expo.dev) bằng điện thoại, hoặc quét mã QR trên trang đó, rồi bấm **Install** để tải file APK.
2. Cài APK. Android hỏi thì cho phép trình duyệt "cài ứng dụng không rõ nguồn gốc".
3. Trên máy tính: `npm start`. Điện thoại và máy tính phải chung Wi-Fi.
4. Mở app **Refound** vừa cài, chọn server đang chạy (hoặc quét mã QR trong terminal). Sửa code là app tự tải lại.

Khác mạng hoặc Wi-Fi chặn kết nối giữa các máy (Wi-Fi trường): chạy `npx expo start --tunnel`.

Chỉ cần cài lại build khi nhóm thêm thư viện native hoặc đổi `app.config.ts`; người phụ trách khung build bằng `npx eas-cli@latest build --profile development --platform android` rồi gửi link mới. Sửa code JS/TS thì không cần.

## Lệnh thường dùng

| Lệnh                     | Việc                                           |
| ------------------------ | ---------------------------------------------- |
| `npm start`              | Chạy dev server                                |
| `npm run lint`           | ESLint                                         |
| `npm run typecheck`      | Kiểm tra kiểu TypeScript                       |
| `npm test`               | Jest                                           |
| `npm run check`          | Chạy cả ba lệnh trên, bắt buộc trước khi mở PR |
| `npm run format`         | Prettier định dạng lại code                    |
| `npx expo install <gói>` | Cài thư viện đúng phiên bản với Expo SDK       |
| `./mvnw spring-boot:run` | Chạy server (trong `server/`)                  |
| `./mvnw test`            | Test server (trong `server/`)                  |

## Cấu trúc thư mục

```
src/
├── app/            # Màn hình (Expo Router): mỗi file là một route
├── features/       # Mỗi tính năng một thư mục: api / hooks / components / utils
│   ├── auth/  items/  matching/
│   └── claims/  notifications/
├── components/ui/  # Component dùng chung (design system)
├── lib/            # API client, query client, cấu hình
├── stores/         # Zustand
├── types/          # Kiểu dữ liệu khớp hợp đồng API của server
├── utils/          # Hàm thuần: điểm khớp, khoảng cách, chuỗi tiếng Việt
└── global.css      # Tailwind (NativeWind)
server/             # Backend Spring Boot (Maven); sẽ tạo theo thiết kế kiến trúc
└── src/main/resources/db/migration/   # Migration Flyway (chỉ sửa bằng file)
docs/               # Kế hoạch và tài liệu
```
