package com.refound.item.domain;

import com.refound.common.GeoPoint;
import java.time.Instant;
import java.util.UUID;

/** Một dòng {@code items} kèm cờ {@code sensitive} của danh mục. Không có {@code ai_caption}: chỉ job embedding dùng. */
public record Item(
        UUID id,
        UUID authorId,
        ItemType type,
        ItemStatus status,
        String category,
        boolean sensitive,
        String description,
        Instant happenedAt,
        GeoPoint location,
        GeoPoint publicLocation,
        Integer radiusM,
        String embeddingStatus,
        String closedReason,
        Instant createdAt,
        Instant updatedAt,
        Instant lastActivityAt,
        Instant deletedAt) {

    public ItemAccess.Facts facts() {
        return new ItemAccess.Facts(authorId, type, status, deletedAt != null);
    }
}
