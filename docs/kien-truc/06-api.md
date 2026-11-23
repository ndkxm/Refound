# 6. Hợp đồng API

## 6.1 Quy ước chung

|            |                                                                                                                                                                                      |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Gốc        | `{EXPO_PUBLIC_API_URL}/api/v1`                                                                                                                                                       |
| Xác thực   | `Authorization: Bearer <JWT>` (HS256, hạn 7 ngày, ADR-05). Mọi endpoint trừ `/auth/*`, `/health`                                                                                     |
| Định dạng  | JSON, `camelCase`. Thời điểm ISO-8601 có múi giờ (`2026-11-20T09:00:00+07:00`). Tọa độ `{ "lat": 10.87, "lng": 106.80 }`                                                             |
| Phân trang | `?cursor=<id>&limit=20` (mặc định 20, tối đa 50), trả `{ items: [...], nextCursor }`                                                                                                 |
| Lỗi        | `ProblemDetail` (RFC 9457, có sẵn trong Spring 6) + trường `code` máy đọc được, `detail` tiếng Việt hiển thị được                                                                    |
| Tài liệu   | `springdoc-openapi` sinh `/v3/api-docs` và Swagger UI ở profile `local`. App sinh kiểu TS bằng `npx openapi-typescript <url> -o src/types/api.d.ts` (devDependency, cần nhóm đồng ý) |

```json
{
  "type": "about:blank",
  "status": 409,
  "code": "INVALID_STATE",
  "title": "Conflict",
  "detail": "Yêu cầu này đã được xử lý."
}
```

| `code`                  | HTTP | Khi nào                                                                          |
| ----------------------- | ---- | -------------------------------------------------------------------------------- |
| `VALIDATION_FAILED`     | 400  | Kèm `errors: [{field, message}]`                                                 |
| `UNAUTHENTICATED`       | 401  | Thiếu/sai/hết hạn token, tài khoản bị khóa                                       |
| `FORBIDDEN`             | 403  | Không có quyền trên tài nguyên công khai                                         |
| `NOT_FOUND`             | 404  | Không tồn tại **hoặc** không được biết là tồn tại (3.3)                          |
| `INVALID_STATE`         | 409  | Chuyển trạng thái không hợp lệ / có người đổi trước                              |
| `DUPLICATE_SUSPECTED`   | 409  | Kèm `existingItemId`                                                             |
| `CLAIM_ALREADY_OPEN`    | 409  | Đã có claim mở trên tin                                                          |
| `HANDOVER_CODE_INVALID` | 409  | Mã sai, hết hạn hoặc đã dùng                                                     |
| `PAYLOAD_TOO_LARGE`     | 413  | Ảnh > 1 MB hoặc request > 6 MB                                                   |
| `RATE_LIMITED`          | 429  | BR-09, gửi tin nhắn quá nhanh; kèm `retryAfterSeconds`                           |
| `ATTEMPT_LIMIT`         | 429  | BR-05                                                                            |
| `TERMS_NOT_ACCEPTED`    | 403  | Chưa đồng ý điều khoản phiên bản hiện hành (03 §3.5)                             |
| `AI_UNAVAILABLE`        | 503  | Chỉ khi tìm kiếm **bằng ảnh** mà Gemini lỗi (tìm bằng chữ thì hạ cấp, không lỗi) |
| `STORAGE_UNAVAILABLE`   | 503  | Không upload được ảnh lên Storage khi đăng tin; DB không ghi gì (7.3)           |

## 6.2 REST

Cột "Quyền" tham chiếu ma trận ở `03-phan-quyen.md`.

### Tài khoản (`auth`)

| Phương thức  | Đường dẫn                | Vào                                                                    | Ra                                                                                                       | Quyền                                            | Mã yêu cầu         |
| ------------ | ------------------------ | ---------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------------------------------------ | ------------------ |
| POST         | `/auth/firebase`         | `{idToken, displayName?}` (Firebase ID token sau khi đăng nhập Google) | `{token, user: MeDto, isNew}`. `isNew=true` → app mở màn đặt tên hiển thị                                | Khách; token sai/hết hạn → 401 `UNAUTHENTICATED` | FR-AUTH-01, ADR-05 |
| GET          | `/health`                | `?db=true`                                                             | `{status, db}`                                                                                           | Khách                                            | Q7                 |
| GET          | `/me`                    |                                                                        | `MeDto` (`authProvider`, `notificationPrefs`, `reputation`, `termsAccepted`, `termsVersion`, `termsUrl`) | Bản thân                                         | FR-AUTH-02         |
| PATCH        | `/me`                    | `{displayName?}`                                                       | `MeDto`                                                                                                  | Bản thân                                         | FR-AUTH-02         |
| PUT          | `/me/avatar`             | multipart `file`                                                       | `MeDto`                                                                                                  | Bản thân                                         | FR-AUTH-02         |
| PUT          | `/me/notification-prefs` | `{match: bool, claim: bool, chat: bool, expiry: bool}`                 | `MeDto`                                                                                                  | Bản thân                                         | FR-NOTI-03         |
| PUT / DELETE | `/me/push-token`         | `{token}`                                                              | 204                                                                                                      | Bản thân                                         | FR-NOTI-01         |
| DELETE       | `/me`                    | `{idToken}` (Firebase ID token mới lấy, xác nhận lại chính chủ)        | 204; app xóa luôn tài khoản Firebase                                                                     | Bản thân                                         | FR-AUTH-04         |
| POST         | `/me/terms`              | `{version}`                                                            | `MeDto`; sai phiên bản → 409 `INVALID_STATE`                                                             | Bản thân                                         | FR-AUTH-05         |
| POST         | `/users/{id}/block`      |                                                                        | 204 (luôn 204 nếu người đó tồn tại, 03 §3.5); tự chặn → 400                                              | Người đăng nhập                                  | FR-AUTH-06, BR-15  |
| DELETE       | `/users/{id}/block`      |                                                                        | 204                                                                                                      | Người đăng nhập (chỉ dòng của mình)              | FR-AUTH-06         |
| GET          | `/me/blocks`             |                                                                        | `[{user: UserPublicDto, blockedAt}]`                                                                     | Bản thân                                         | FR-AUTH-06         |
| GET          | `/users/{id}`            |                                                                        | `{id, displayName, avatarUrl, reputation}`                                                               | Người đăng nhập                                  | FR-AUTH-02         |
| GET          | `/me/reputation-events`  |                                                                        | trang `ReputationEventDto`                                                                               | Bản thân                                         | FR-CLAIM-05        |

### Tin (`item`)

| Phương thức | Đường dẫn                | Vào                                                                                                                                                           | Ra                                                          | Quyền                         | Mã yêu cầu                  |
| ----------- | ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------- | ----------------------------- | --------------------------- |
| GET         | `/categories`            |                                                                                                                                                               | `[{code, nameVi, sensitive}]`                               | Đăng nhập                     |                             |
| POST        | `/items?force=`          | multipart: `data` (chuỗi JSON `{type, category, description, happenedAt, location, radiusM?, question?, answer?}`, không cần Content-Type), `full`, `thumb` (phần lặp lại 0–4 lần, cặp thứ i là ảnh vị trí i; JPEG ≤ 1 MB) | 201 `ItemOwnerDto`                                          | Đăng nhập, BR-09              | FR-ITEM-01, 02, FR-MATCH-04 |
| GET         | `/items`                 | `?type&category&from&to&bbox=minLng,minLat,maxLng,maxLat&cursor`                                                                                              | trang `ItemSummaryDto` (thumb signed URL, `publicLocation`) | Đăng nhập                     | FR-ITEM-03                  |
| GET         | `/items/{id}`            |                                                                                                                                                               | `ItemPublicDto` hoặc `ItemOwnerDto` (theo người xem)        | 3.2                           | FR-ITEM-04, 07              |
| GET         | `/me/items`              | `?status`                                                                                                                                                     | trang `ItemOwnerDto`                                        | Bản thân                      | FR-ITEM-05                  |
| PATCH       | `/items/{id}`            | `{description?, category?, happenedAt?, location?, radiusM?, question?, answer?}`                                                                             | `ItemOwnerDto`                                              | Tác giả                       | FR-ITEM-05                  |
| POST        | `/items/{id}/close`      | `{reason}`                                                                                                                                                    | `ItemOwnerDto`                                              | Tác giả                       | FR-ITEM-05                  |
| POST        | `/items/{id}/mark-found` |                                                                                                                                                               | `ItemOwnerDto`                                              | Tác giả tin `lost`            |                             |
| POST        | `/items/{id}/renew`      |                                                                                                                                                               | `ItemOwnerDto`                                              | Tác giả                       | BR-07                       |
| DELETE      | `/items/{id}`            |                                                                                                                                                               | 204                                                         | Tác giả                       | FR-ITEM-05                  |
| GET         | `/items/{id}/secret`     |                                                                                                                                                               | `{question, answer}`                                        | Tác giả tin `found`           | BR-02                       |
| POST        | `/items/{id}/reports`    | `{reason}`                                                                                                                                                    | 201                                                         | Đăng nhập, không phải tác giả | FR-ITEM-06                  |

### Đối chiếu (`matching`)

| Phương thức | Đường dẫn                       | Vào                                                                                        | Ra                                                                                          | Quyền                     | Mã yêu cầu      |
| ----------- | ------------------------------- | ------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------- | ------------------------- | --------------- |
| POST        | `/search`                       | multipart: `data` (`{type, text?, location?, radiusM?, happenedAt?, category?}`), `image?` | `{results: [{item: ItemSummaryDto, score, components:{v,d,t,c}, reasons:[...]}], degraded}` | Đăng nhập                 | FR-MATCH-01, 02 |
| GET         | `/items/{id}/matches`           |                                                                                            | `[{matchId, otherItem, score, reasons, source}]`                                            | Tác giả tin               | FR-MATCH-03     |
| POST        | `/matches/{id}/dismiss`         |                                                                                            | 204                                                                                         | Tác giả một trong hai tin |                 |
| POST        | `/items/{lostId}/found-reports` | `{foundItemId}`                                                                            | 201 `{matchId}`                                                                             | Tác giả của `foundItemId` | FR-MATCH-06     |

### Yêu cầu nhận và chat (`claim`, `chat`)

| Phương thức | Đường dẫn                      | Vào                             | Ra                                                                                                                                  | Quyền                                             | Mã yêu cầu         |
| ----------- | ------------------------------ | ------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------- | ------------------ |
| GET         | `/items/{id}/question`         |                                 | `{question}`                                                                                                                        | Đăng nhập, tin `found` `open`, không phải tác giả | FR-CLAIM-01        |
| POST        | `/items/{id}/claims`           | `{answer, lostItemId?}`         | 201 `ClaimDto`                                                                                                                      | C1                                                | FR-CLAIM-01, BR-05 |
| GET         | `/claims`                      | `?role=claimant\|finder&status` | trang `ClaimSummaryDto`                                                                                                             | Bản thân                                          |                    |
| GET         | `/claims/{id}`                 |                                 | `ClaimDto` (`answer`, `status`, `otherParty{displayName, reputation}`, `exactLocation` nếu `accepted`, ảnh nhạy cảm nếu `accepted`) | Hai bên                                           | FR-CLAIM-02        |
| POST        | `/claims/{id}/accept`          |                                 | `ClaimDto`                                                                                                                          | Finder (C2)                                       | FR-CLAIM-02        |
| POST        | `/claims/{id}/reject`          | `{reason}`                      | `ClaimDto`                                                                                                                          | Finder (C3)                                       | FR-CLAIM-02        |
| POST        | `/claims/{id}/withdraw`        |                                 | `ClaimDto`                                                                                                                          | Owner (C4)                                        |                    |
| POST        | `/claims/{id}/cancel`          | `{reason, reportViolation?}`    | `ClaimDto`                                                                                                                          | Hai bên (C5)                                      | FR-CLAIM-06        |
| POST        | `/claims/{id}/dispute`         | `{reason}`                      | `ClaimDto`                                                                                                                          | Hai bên (C7)                                      | FR-CLAIM-06        |
| POST        | `/claims/{id}/handover-code`   |                                 | `{code, expiresAt}` (app vẽ QR từ chuỗi `refound:handover:{claimId}:{code}`)                                                        | Finder (C5b)                                      | FR-CLAIM-04        |
| POST        | `/claims/{id}/handover`        | `{code}`                        | `ClaimDto`                                                                                                                          | Owner (C6)                                        | FR-CLAIM-04, BR-06 |
| POST        | `/claims/{id}/manual-complete` | `{reason}`                      | `ClaimDto`                                                                                                                          | Finder (C6b)                                      | BR-06              |
| GET         | `/claims/{id}/messages`        | `?after=<id>&limit`             | `[{id, mine, body, createdAt}]`                                                                                                     | Hai bên                                           | FR-CLAIM-03        |
| POST        | `/claims/{id}/messages`        | `{body}` (1–1000 ký tự)         | 201 `MessageDto`                                                                                                                    | Hai bên, claim `accepted`                         | FR-CLAIM-03        |

Tin nhắn trả `mine: true/false`, **không** trả `senderId`; server vốn không lưu email (BR-04).

### Thông báo (`notification`)

| Phương thức | Đường dẫn                                             | Ra                                                                                        | Mã yêu cầu |
| ----------- | ----------------------------------------------------- | ----------------------------------------------------------------------------------------- | ---------- |
| GET         | `/notifications?cursor`                               | trang `{id, type, title, body, payload: {screen, id}, readAt, createdAt}` + `unreadCount` | FR-NOTI-02 |
| POST        | `/notifications/{id}/read`, `/notifications/read-all` | 204                                                                                       | FR-NOTI-02 |

### Quản trị (`/admin`, role ADMIN)

| Phương thức | Đường dẫn                                               | Mã yêu cầu  |
| ----------- | ------------------------------------------------------- | ----------- |
| GET         | `/admin/reports?status=open`                            | FR-ITEM-06  |
| POST        | `/admin/items/{id}/restore`, `/admin/items/{id}/remove` | BR-10       |
| POST        | `/admin/claims/{id}/resolve` `{faultUserId?}`           | FR-CLAIM-06 |
| POST        | `/admin/users/{id}/lock`, `/unlock`                     | FR-AUTH-03  |

### P2 (chỉ làm nếu qua điểm cắt, `10-ke-hoach-3-tuan.md`)

| Phương thức         | Đường dẫn                                                             | Mã yêu cầu |
| ------------------- | --------------------------------------------------------------------- | ---------- |
| GET                 | `/stats/heatmap?hour=` → `[{lat, lng, count}]` ô 100 m, chỉ ô ≥ 3 tin | FR-HEAT-01 |
| POST / GET / DELETE | `/tags`, `/tags/{id}`; `POST /tags/scan` `{code, location, message}`  | FR-TAG-01  |

## 6.3 WebSocket

|               |                                                                                                                                                                     |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Endpoint      | `wss://.../ws` (STOMP qua WebSocket thuần, không SockJS)                                                                                                            |
| Xác thực      | Header `Authorization: Bearer <JWT>` trong frame `CONNECT`; `ChannelInterceptor` kiểm tra, gán `Principal = userId`                                                 |
| Kênh duy nhất | Client `SUBSCRIBE /user/queue/events`. Server gửi bằng `convertAndSendToUser(userId, "/queue/events", evt)`, nên **không có kênh nào cần kiểm tra quyền subscribe** |
| Client gửi    | Không. Mọi ghi đi qua REST (một chỗ kiểm tra quyền, một chỗ test)                                                                                                   |
| Heartbeat     | 20 s / 20 s                                                                                                                                                         |

Sự kiện (chỉ là tín hiệu; app nhận xong thì `invalidateQueries`):

```json
{ "type": "message.created", "claimId": "…", "messageId": 123 }
{ "type": "claim.updated",   "claimId": "…", "status": "accepted" }
{ "type": "notification.created", "notificationId": 456 }
```

Ngoài sự kiện WS, server luôn gửi Expo Push nếu loại thông báo đó đang bật; app đang mở thì tự ẩn banner (ADR-08).
