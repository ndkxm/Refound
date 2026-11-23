package com.refound.item.application;

import com.refound.common.GeoPoint;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO tách theo người xem (03 §3.3, ADR-04): bản công khai không có trường vị trí chính xác nào, nên không thể lộ
 * nhầm. Không DTO nào ở đây chứa đáp án câu hỏi kín ({@link SecretDto} chỉ trả qua {@code /items/{id}/secret}).
 */
public final class ItemDtos {

    private ItemDtos() {}

    /** URL ký hạn 1 giờ (ADR-06); null nếu Storage tạm lỗi lúc ký. */
    public record ImageDto(UUID id, int position, String fullUrl, String thumbUrl) {}

    /** Tác giả và admin (03 §3.2: "mọi trạng thái của mình, vị trí chính xác"). */
    public record ItemOwnerDto(
            UUID id,
            UUID authorId,
            String type,
            String status,
            String category,
            boolean sensitive,
            String description,
            OffsetDateTime happenedAt,
            GeoPoint exactLocation,
            GeoPoint publicLocation,
            Integer radiusM,
            List<ImageDto> images,
            String embeddingStatus,
            String closedReason,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime lastActivityAt) {}

    /** Người khác: chỉ vị trí công khai (BR-11); tin nhạy cảm không có ảnh (BR-08, X5). */
    public record ItemPublicDto(
            UUID id,
            UUID authorId,
            String type,
            String status,
            String category,
            boolean sensitive,
            String description,
            OffsetDateTime happenedAt,
            GeoPoint publicLocation,
            Integer radiusM,
            List<ImageDto> images,
            OffsetDateTime createdAt) {}

    /** Một dòng danh sách, bản đồ, kết quả tìm kiếm: ảnh nhỏ đầu tiên, null nếu không có ảnh hoặc tin nhạy cảm. */
    public record ItemSummaryDto(
            UUID id,
            String type,
            String status,
            String category,
            boolean sensitive,
            String description,
            OffsetDateTime happenedAt,
            GeoPoint publicLocation,
            Integer radiusM,
            String thumbUrl,
            OffsetDateTime createdAt) {}

    /** {@code GET /items/{id}/secret}: chỉ tác giả tin {@code found} (BR-02, X6). */
    public record SecretDto(String question, String answer) {}

    public record CategoryDto(String code, String nameVi, boolean sensitive) {}

    /** Trang theo con trỏ (06 §6.1); {@code nextCursor} null là hết. */
    public record Page<T>(List<T> items, String nextCursor) {}
}
