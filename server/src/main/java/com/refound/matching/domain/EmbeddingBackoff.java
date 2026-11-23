package com.refound.matching.domain;

import java.time.Duration;
import java.util.Optional;

/**
 * Lùi dần khi tạo embedding lỗi (04 §4.1, 7.3), hàm thuần: thử lại sau 1, 2, 4, 8, 16, 32 phút; lỗi lần thứ 7 thì
 * {@code failed} (tin vẫn hiện, chỉ đối chiếu bằng vị trí, thời gian, danh mục).
 */
public final class EmbeddingBackoff {

    public static final int MAX_RETRIES = 6;

    private EmbeddingBackoff() {}

    /**
     * @param failures số lần lỗi tính cả lần vừa rồi ({@code embedding_attempts} mới, ≥ 1)
     * @return thời gian chờ tới lần thử sau, hoặc rỗng nếu đã hết lượt
     */
    public static Optional<Duration> retryDelay(int failures) {
        if (failures > MAX_RETRIES) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofMinutes(1L << (Math.max(failures, 1) - 1)));
    }
}
