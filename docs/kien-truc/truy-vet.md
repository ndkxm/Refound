# Bảng truy vết yêu cầu → thiết kế

Cột "Thành phần" ghi lớp server (`module.lớp`) và feature app. "Thiết kế" trỏ tới mục trong `docs/kien-truc/`. Trạng thái: ✅ đáp ứng · ◐ đáp ứng có điều kiện/đã diễn giải · ✂ đề xuất cắt.

## Chức năng

| Mã          | Ưu  | Thành phần                                                                           | Thiết kế                  | Test                    | TT                     |
| ----------- | --- | ------------------------------------------------------------------------------------ | ------------------------- | ----------------------- | ---------------------- |
| FR-AUTH-01  | P0  | `auth.AuthService`, `FirebaseTokenVerifier`, `JwtIssuer`; app `auth` (Firebase Auth) | 6.2, ADR-05               | X19, X23                | ✅ Google (BRD v0.3)   |
| FR-AUTH-02  | P0  | `auth`, `GET /users/{id}`; app `profile`                                             | 6.2, 3.2                  | X17                     | ✅                     |
| FR-AUTH-03  | P1  | `SecurityFilterChain`, `UserStatusFilter`, `@PreAuthorize` admin                     | 3.3                       | X18, X19                | ✅                     |
| FR-AUTH-04  | P1  | `auth.AuthService.deleteAccount`                                                     | 6.2, V16                  | IT                      | ◐                      |
| FR-AUTH-05  | P1  | `UserStatusFilter`, `auth.ProfileService.acceptTerms`; app màn điều khoản            | 3.5, 6.2                  | X29                     | ✅                     |
| FR-AUTH-06  | P1  | `moderation.BlockService`; lọc ở truy vấn tin, đối chiếu, claim                      | 3.5, 5.2 C10, 6.2         | X24–X28                 | ✅                     |
| FR-ITEM-01  | P0  | `item.ItemService.create`, `SupabaseStorageClient`; app `items`                      | 1 luồng 1, ADR-06, ADR-14 | `item-form-schema`, IT  | ✅                     |
| FR-ITEM-02  | P0  | như trên, `radius_m`                                                                 | 2.2                       | `item-form-schema`      | ✅                     |
| FR-ITEM-03  | P0  | `GET /items` (bbox, lọc); app `(tabs)/index`                                         | 6.2                       | IT                      | ✅                     |
| FR-ITEM-04  | P0  | DTO theo người xem                                                                   | 3.3                       | X3, X6                  | ✅                     |
| FR-ITEM-05  | P0  | `ItemStateMachine`, `ItemAccess`                                                     | 5.1 I2, I7, I13           | X1, X2                  | ✅                     |
| FR-ITEM-06  | P1  | `moderation`, `reports`                                                              | 5.1 I10–I12               | IT                      | ◐ (V4)                 |
| FR-ITEM-07  | P1  | `ItemDto` ký URL theo quyền                                                          | 3.2, ADR-06               | X5                      | ◐ (V13)                |
| FR-MATCH-01 | P0  | `matching.SearchService`, `embedding_cache`                                          | 4.3                       | IT, eval                | ✅                     |
| FR-MATCH-02 | P0  | `matching.domain.MatchScorer`, `MatchReasons`                                        | 4.2                       | `MatchScorerTest`       | ✅                     |
| FR-MATCH-03 | P1  | `EmbeddingJob`, `MatchingService`, `match_candidates`                                | 4.4, ADR-01               | IT                      | ✅                     |
| FR-MATCH-04 | P1  | `item.domain.DuplicateRule`                                                          | 4.5, ADR-18               | `DuplicateRuleTest`     | ✅                     |
| FR-MATCH-05 | P0  | `eval/`, `@Tag("eval")`                                                              | 4.6                       | eval                    | ✅                     |
| FR-MATCH-06 | P1  | `POST /items/{lostId}/found-reports`                                                 | 4.4                       | X8                      | ✅                     |
| FR-CLAIM-01 | P1  | `claim.ClaimService.submit`, partial unique index                                    | 5.2 C1                    | X9, X10                 | ✅                     |
| FR-CLAIM-02 | P1  | `ClaimStateMachine`, `AttemptPolicy`                                                 | 5.2 C2, C3, 5.3           | X12                     | ✅                     |
| FR-CLAIM-03 | P1  | `chat.ChatService`, WS `/user/queue/events`                                          | 6.3, ADR-07               | X14, X15, X21           | ◐ (V17)                |
| FR-CLAIM-04 | P1  | `claim.domain.HandoverCode`, `handovers`                                             | 5.2 C5b, C6               | `HandoverCodeTest`, X13 | ✅                     |
| FR-CLAIM-05 | P1  | `moderation` (uy tín), `reputation_events`                                           | 5.2 C6, C8                | IT, X22                 | ◐ (V5)                 |
| FR-CLAIM-06 | P1  | `ClaimService.cancel/dispute`, `admin resolve`                                       | 5.2 C5, C7, C8            | X12                     | ◐ (V15)                |
| FR-NOTI-01  | P1  | `notification.NotificationService`, `ExpoPushClient`                                 | ADR-08                    | IT                      | ✅                     |
| FR-NOTI-02  | P1  | `notifications`, app `inbox`                                                         | 6.2                       | X16                     | ✅                     |
| FR-NOTI-03  | P1  | `users.notification_prefs`                                                           | 6.2                       | IT                      | ✅                     |
| FR-TAG-01   | P2  | `tags`                                                                               | 6.2 P2, 10.4              | —                       | ◐ chỉ nếu qua điểm cắt |
| FR-TAG-02   | P2  | —                                                                                    | 10.4                      | —                       | ✂ đã cắt               |
| FR-GEO-01   | P2  | —                                                                                    | 10.4                      | —                       | ✂ đã cắt               |
| FR-HEAT-01  | P2  | `GET /stats/heatmap`                                                                 | 6.2 P2                    | —                       | ◐ chỉ nếu qua điểm cắt |

## Quy tắc nghiệp vụ

| Mã    | Thành phần                                                        | Thiết kế         | Test                       | TT      |
| ----- | ----------------------------------------------------------------- | ---------------- | -------------------------- | ------- |
| BR-01 | Lọc `type` khác nhau trong SQL đối chiếu; `CHECK`                 | 4.2              | IT                         | ✅      |
| BR-02 | Bảng `verification_secrets`, `FinderSecretDto` duy nhất           | 2.2, 3.3, ADR-17 | X6                         | ✅      |
| BR-03 | Chỉ Finder gọi `accept`; không so chuỗi                           | 5.2 C2           | X12                        | ✅      |
| BR-04 | Server không lưu email/SĐT; tin nhắn trả `mine`                   | 6.2              | X17, X14                   | ✅      |
| BR-05 | `AttemptPolicy`                                                   | 5.3              | `AttemptPolicyTest`        | ◐ (V5)  |
| BR-06 | C6 (QR), C6b (tự xác nhận)                                        | 5.2              | `ClaimStateMachineTest`    | ✅      |
| BR-07 | `@Scheduled` + cron ngày                                          | 5.1 I8, I8a, I9  | IT với `Clock` giả         | ◐ (V11) |
| BR-08 | Job bỏ ảnh nhạy cảm; ký URL theo quyền                            | 4.1, ADR-06      | X5, IT job                 | ✅      |
| BR-09 | Đếm trong `ItemService.create`                                    | 7.4              | IT                         | ✅      |
| BR-10 | `moderation`, I10                                                 | 5.1              | IT                         | ◐ (V4)  |
| BR-11 | `LocationRounding`, `public_location`                             | 2.3              | `LocationRoundingTest`, X3 | ✅      |
| BR-12 | `timestamptz`; `format-time.ts`                                   | 2                | `format-time.test.ts`      | ✅      |
| BR-13 | Tin `lost` không có câu hỏi; vị trí là khu vực                    | 2.2, 2.3         | IT                         | ✅      |
| BR-14 | Báo khớp chủ động chỉ tạo `match_candidates`, claim vẫn qua C1–C2 | 4.4              | IT                         | ✅      |

## Phi chức năng

| Mã     | Thành phần                                     | Thiết kế    | TT                        |
| ------ | ---------------------------------------------- | ----------- | ------------------------- |
| NFR-01 | `*Access` + test X1–X23                        | 3           | ✅                        |
| NFR-02 | Bí mật chỉ ở env server; schema `refound`      | ADR-10, 7   | ✅                        |
| NFR-03 | BR-02/04/08/11/13 như trên                     | 3.2         | ✅                        |
| NFR-04 | Quét tuần tự < 50 ms; cache vector truy vấn    | ADR-03, 4.3 | ◐ (V17)                   |
| NFR-05 | Hàng đợi trên `items`, lùi dần, circuit 5 phút | 7.3, ADR-01 | ✅                        |
| NFR-06 | Bảng 7.1, 7.2                                  | 7           | ◐ chờ U1, U2              |
| NFR-07 | Lệnh seed có vector sẵn; kịch bản dự phòng     | 9.3         | ✅                        |
| NFR-08 | Persist cache TanStack Query, `draft-store`    | 8.1         | ✅ (cắt đầu tiên nếu trễ) |
| NFR-09 | JUnit cho `domain`, Jest cho `utils`           | 8, 9        | ✅                        |
| NFR-10 | `detail` lỗi tiếng Việt; UI                    | 6.1         | ✅                        |
| NFR-11 | `components/ui` (vùng chạm 44dp)               | 8.1         | ✅                        |
| NFR-12 | Thêm `org_id` bằng migration sau               | 2           | ✅                        |
| NFR-13 | 1 instance, pool 3, quy mô 7.1                 | 7           | ◐ chờ U5                  |
