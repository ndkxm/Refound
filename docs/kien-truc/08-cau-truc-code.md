# 8. Cấu trúc code

## 8.1 App (`src/`)

```
src/
  app/                         # route, chỉ ghép UI
    _layout.tsx                # QueryClientProvider, warmup /health, WS
    (auth)/login.tsx           # nút "Đăng nhập bằng Google"
    (auth)/profile-setup.tsx   # đặt tên hiển thị lần đầu (isNew)
    (tabs)/index.tsx           # bản đồ + danh sách
    (tabs)/search.tsx
    (tabs)/post.tsx            # chọn "Tôi nhặt được" / "Tôi bị mất"
    (tabs)/inbox.tsx           # thông báo
    (tabs)/profile.tsx
    items/new.tsx              # ?type=found|lost
    items/[id]/index.tsx, edit.tsx, matches.tsx
    claims/index.tsx
    claims/[id]/index.tsx      # chi tiết + chat
    claims/[id]/handover.tsx   # Finder hiện QR / Owner quét
    settings/notifications.tsx, settings/server.tsx (ẩn)
    admin/reports.tsx
  features/<module>/{api, hooks, components, utils}
  components/ui/               # design system dùng chung
  lib/api-client.ts            # fetch + token + timeout + ProblemDetail → ApiError
  lib/query-client.ts          # retry policy (7.5), persist cache (NFR-08)
  lib/ws-client.ts             # STOMP, nối lại, invalidate
  stores/auth-store.ts         # token, user (Zustand + secure-store)
  stores/draft-store.ts        # bản nháp form đăng tin (persist)
  types/api.d.ts               # sinh từ OpenAPI
```

Ranh giới trong mỗi feature:

| Thư mục       | Được làm                                                    | Không được làm         |
| ------------- | ----------------------------------------------------------- | ---------------------- |
| `api/`        | Hàm gọi REST, trả kiểu từ `src/types`                       | Gọi hook, giữ state    |
| `hooks/`      | `useQuery`/`useMutation` bọc `api/`, khóa query, invalidate | JSX                    |
| `components/` | UI của feature, dùng `components/ui`                        | Gọi `fetch` trực tiếp  |
| `utils/`      | Hàm thuần, có test cạnh file                                | Import React, gọi mạng |

Ánh xạ yêu cầu → feature (app):

| Feature         | Yêu cầu                        | Hàm thuần cần test (Jest)                                                                                                                                           |
| --------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `auth`          | FR-AUTH-01..04                 | `profile-schema.ts` (zod: tên 2–40)                                                                                                                                 |
| `items`         | FR-ITEM-01..07, BR-11 hiển thị | `item-form-schema.ts` (found ≥ 1 ảnh + câu hỏi; lost nhạy cảm 0 ảnh; bán kính 50–2000), `resize-plan.ts` (tính kích thước 1024/512 giữ tỉ lệ), `format-distance.ts` |
| `matching`      | FR-MATCH-01, 03 (hiển thị), 06 | `score-label.ts` (điểm → "Rất giống / Có thể / Ít khả năng")                                                                                                        |
| `claims`        | FR-CLAIM-01..06, chat          | `claim-actions.ts` (trạng thái + vai trò → nút được hiện; bản sao **chỉ để hiển thị** của bảng 5.2, server vẫn quyết), `parse-handover-qr.ts`                       |
| `notifications` | FR-NOTI-01..03                 | `notification-route.ts` (payload → đường dẫn màn hình)                                                                                                              |
| `lib`           | NFR-05, 08, Q7                 | `is-retryable.ts`, `format-time.ts` (Asia/Ho_Chi_Minh)                                                                                                              |

Lưu ý: điểm khớp, máy trạng thái, phân quyền **không** nằm ở app. Thư mục `src/features/matching/` chỉ hiển thị kết quả (xem `van-de-phat-hien.md` V1).

### Thư viện cần thêm (một đợt, ngày 1 tuần 1, nhóm duyệt)

Gói native, cài bằng `npx expo install`, build lại development build **một lần cho cả nhóm**:

| Gói                                                         | Dùng cho                                                     | Có cách tránh không                                                     |
| ----------------------------------------------------------- | ------------------------------------------------------------ | ----------------------------------------------------------------------- |
| `expo-image-picker`                                         | Chụp/chọn ảnh                                                | Không                                                                   |
| `expo-image-manipulator`                                    | Resize trước khi upload                                      | Không (resize ở server tốn RAM 512 MB)                                  |
| `expo-location`                                             | Vị trí hiện tại                                              | Không                                                                   |
| `expo-camera`                                               | Quét QR bàn giao                                             | Không                                                                   |
| `@maplibre/maplibre-react-native`                           | Bản đồ (đã chốt)                                             | Không                                                                   |
| `expo-secure-store`                                         | Lưu JWT, URL server dự phòng                                 | Không                                                                   |
| `@react-native-async-storage/async-storage`                 | Persist cache TanStack Query, bản nháp (NFR-08)              | Có thể bỏ nếu cắt NFR-08                                                |
| `react-native-svg`                                          | Vẽ QR (cùng `react-native-qrcode-svg`)                       | Không                                                                   |
| `@react-native-firebase/app`, `@react-native-firebase/auth` | Đăng nhập Google qua Firebase (ADR-05)                       | Có: server kiểm tra thẳng Google ID token; nhóm chọn giữ Firebase (D10) |
| `@react-native-google-signin/google-signin`                 | Lấy credential Google để đưa vào Firebase                    | Có thể thay bằng `expo-auth-session`, nhưng luồng trình duyệt kém hơn   |
| `expo-device`                                               | Kiểm `Device.isDevice` trước khi lấy push token              | Không                                                                   |
| `@react-native-community/netinfo`                           | `onlineManager` của TanStack Query (NFR-08)                  | Có thể dùng `expo-network`                                              |
| `react-native-keyboard-controller`                          | Bàn phím không che ô nhập (chat, form) khi Android tràn viền | Không ổn với `KeyboardAvoidingView`                                     |

Gói JS thuần (không cần build lại): `@tanstack/react-query`, `@tanstack/query-async-storage-persister`, `zustand`, `react-hook-form`, `zod`, `@hookform/resolvers`, `@stomp/stompjs`, `react-native-qrcode-svg`; dev: `openapi-typescript`.

## 8.2 Server (`server/`)

```
server/src/main/java/com/refound/
  common/        config (SecurityConfig, WebSocketConfig, JacksonConfig), ApiExceptionHandler, Clock
  auth/          presentation · application (AuthService, JwtIssuer)
                 infrastructure: FirebaseTokenVerifier (interface IdentityVerifier), UserRepository
  item/          ... domain: ItemStateMachine, ItemAccess, LocationRounding, DuplicateRule
                     infrastructure: ItemRepository, SupabaseStorageClient
  matching/      ... domain: MatchScorer, MatchWeights, MatchReasons
                     application: EmbeddingJob, MatchingService, SearchService
                     infrastructure: EmbeddingClient (interface, có bản giả cho test), GeminiEmbeddingClient,
                     VisionClient (interface, có bản giả), GroqVisionClient
  claim/         ... domain: ClaimStateMachine, ClaimAccess, AttemptPolicy, HandoverCode
                     application: ClaimService, HandoverService
  chat/          ... ChatService, ChatController, WsEventPublisher
  notification/  ... NotificationService, ExpoPushClient, listener AFTER_COMMIT
  moderation/    ... reports, uy tín, controller /admin   (module mới, đã thêm vào AGENTS.md)
server/src/main/resources/
  application.yml, application-local.yml (không commit bí mật), db/migration/
server/src/test/java/...       cùng cấu trúc; *Test (unit), *AccessTest, *IT (tích hợp)
server/src/test/resources/eval/
```

Quy tắc gọi giữa module:

| Loại phụ thuộc                              | Cách                                                                | Ví dụ                                                      |
| ------------------------------------------- | ------------------------------------------------------------------- | ---------------------------------------------------------- |
| Đổi trạng thái phải cùng transaction        | Gọi thẳng service public của module kia                             | `ClaimService.complete()` gọi `ItemService.markReturned()` |
| Tác dụng phụ ra ngoài (push, WS, thông báo) | Phát sự kiện, nghe bằng `@TransactionalEventListener(AFTER_COMMIT)` | `ClaimAcceptedEvent` → `NotificationService`               |
| Đọc dữ liệu module khác                     | Gọi service, không dùng repository của module khác                  | `MatchingService` gọi `ItemQueryService`                   |

Hàm thuần cần JUnit (không Spring, không DB): `MatchScorer`, `MatchReasons`, `ItemStateMachine`, `ClaimStateMachine`, `ItemAccess`, `ClaimAccess`, `AttemptPolicy`, `LocationRounding`, `DuplicateRule`, `HandoverCode` (sinh, băm, so sánh hằng thời gian, hết hạn với `Clock` giả).
