package com.refound.item.application;

import java.util.UUID;

/**
 * Tin mới hoặc vừa đổi mô tả/danh mục, đang chờ embedding (I1, I2). Phát trong transaction ghi tin; job embedding
 * nghe sau commit để làm ngay thay vì đợi lượt vét 30 giây (ADR-01).
 */
public record ItemContentChanged(UUID itemId) {}
