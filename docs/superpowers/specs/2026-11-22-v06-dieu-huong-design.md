# V06 · Khung điều hướng — thiết kế ngắn

## Mục tiêu

Tiêu chí của V06: **mở app là vào đúng màn theo trạng thái đã đăng nhập chưa, đã onboarding chưa.**
Đây là khung mà cả 6 người gắn màn của mình vào, nên cấu trúc route phải ổn định từ ngày 3.

- **Làm:** cây route (auth) / (onboarding) / (tabs) 5 tab / modal Ghi; chặn màn theo trạng thái; thanh tab hoàn chỉnh; màn tạm cho từng tab; store phiên mà V08, V09 nối vào.
- **Không làm:** form đăng nhập (V08), các bước onboarding (V09), nội dung từng tab, design system đầy đủ (V07).
- **Không được:** màn tạm trông như app hỏng; mẫu UI kiểu AI (gradient tím, kính mờ, emoji làm icon, card đổ bóng khắp nơi).

## Các cách đã cân nhắc

| Cách                                     | Được                                                                                                          | Mất                                                            |
| ---------------------------------------- | ------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| **`Stack.Protected` + tab JS (đã chọn)** | Expo Router tự chuyển màn khi trạng thái đổi, không tự viết redirect; tab JS cho phép nút Ghi ở giữa mở modal | Thanh tab vẽ bằng JS, không phải thanh tab native của Android  |
| Tự redirect trong từng `_layout`         | Chạy được ở SDK cũ                                                                                            | Dễ lọt màn, nhấp nháy màn sai khi mở app; SDK 58 không cần nữa |
| Tab native (`expo-router/native-tabs`)   | Nhìn chuẩn Android                                                                                            | Không có cách cho một tab mở modal thay vì chuyển tab          |

## Cây route

```
src/app/
├── _layout.tsx            # Stack gốc: Protected theo trạng thái, giữ splash khi đang tải phiên
├── (auth)/
│   ├── _layout.tsx
│   ├── sign-in.tsx        # màn tạm; V08 thay form thật
│   └── sign-up.tsx
├── (onboarding)/
│   ├── _layout.tsx
│   └── index.tsx          # màn tạm; V09 thay 4 bước thật
├── (tabs)/
│   ├── _layout.tsx        # thanh tab 5 ô
│   ├── index.tsx          # Hôm nay
│   ├── transactions/index.tsx   # Sổ
│   ├── record.tsx         # chỉ để giữ chỗ ô Ghi, bấm vào mở /capture
│   ├── map.tsx            # Bản đồ
│   └── groups/index.tsx   # Nhóm
└── capture/
    ├── _layout.tsx
    └── index.tsx          # modal Ghi: 4 cách Nói · Gõ câu · Chụp/chọn ảnh · Nhập tay
```

Quy tắc chặn (một hàm thuần `getRouteAccess` trong `src/features/auth/utils/`, có unit test):

| Trạng thái                    | Được vào                  |
| ----------------------------- | ------------------------- |
| đang tải phiên                | không màn nào, giữ splash |
| chưa đăng nhập                | (auth)                    |
| đã đăng nhập, chưa onboarding | (onboarding)              |
| đã đăng nhập, đã onboarding   | (tabs), capture           |

## Trạng thái

`src/stores/session-store.ts` (Zustand): `status: 'loading' | 'signed-out' | 'signed-in'` và `hasOnboarded: boolean`, cùng các hàm `signIn`, `signOut`, `completeOnboarding`.
Hiện chưa có Supabase nên mở app là `signed-out`. Màn tạm của (auth) và (onboarding) có nút **chỉ hiện khi dev** (`__DEV__`) để đi hết luồng trên máy.
V08 chỉ cần gọi `signIn`/`signOut` từ sự kiện của Supabase Auth; V09 gọi `completeOnboarding` sau khi lưu `profiles`.

## Giao diện

Định hướng: **sổ tay giấy ấm**, hợp tên "Ví Lười" (ghi chép nhẹ nhàng, không giống app ngân hàng).

- Nền giấy ngà, chữ màu mực gần đen, **một màu nhấn duy nhất**: xanh lá đậm cho "còn tiêu được". Vàng và đỏ dành cho trạng thái safe-to-spend (F21), không dùng để trang trí.
- Thanh tab phẳng, viền trên mảnh, không đổ bóng. Ô đang chọn: icon và chữ màu mực, các ô khác màu xám. Icon Material Symbols qua `expo-symbols`.
- **Nút Ghi ở giữa:** hình tròn màu mực, dấu cộng trắng, nhô lên khỏi thanh tab. Bấm thì rung nhẹ (`expo-haptics`) và mở modal Ghi trượt từ dưới lên.
- Màn tạm của mỗi tab: tiêu đề lớn, một câu nói màn này sẽ có gì và việc nào làm nó (ví dụ "V25 · TV5"). Nhìn như một trang sổ trống có chủ đích, không như lỗi.
- Có chế độ tối: nền mực, chữ giấy.
- Màu đặt thành token trong `tailwind.config.js` (`paper`, `ink`, `accent`, `muted`, `line`); thanh tab đọc cùng token đó. V07 mở rộng tiếp, thêm font Be Vietnam Pro.

```
┌──────────────────────────────────┐
│ Hôm nay                          │
│                                  │
│ Con số còn được tiêu sẽ ở đây.   │
│ V25 · TV5                        │
│                                  │
├──────────────────────────────────┤
│  ☀       ▤      (＋)     ◫     ⚇  │
│ Hôm nay  Sổ     Ghi   Bản đồ Nhóm │
└──────────────────────────────────┘
```

## Kiểm thử

- Unit test `getRouteAccess` cho cả 4 trạng thái.
- Test router bằng `expo-router/testing-library` (cần thêm `@testing-library/react-native` vào devDependencies, là thư viện JS nên không phải build lại): chưa đăng nhập thì vào `/sign-in`; đăng nhập mà chưa onboarding thì vào `/onboarding`; xong cả hai thì vào `/`; đăng xuất thì về `/sign-in`.
- Thử tay trên máy Android: đi hết luồng, bấm nút Ghi, nút back đóng modal, chế độ tối.

## Thư viện thêm

`zustand` (state client, đã có trong stack của kế hoạch) và `@testing-library/react-native` (chỉ dùng để test). Cả hai đều là JS.

## Để người làm tự quyết

Tên icon Material cụ thể, khoảng cách và cỡ chữ chi tiết, câu chữ trên các màn tạm.
