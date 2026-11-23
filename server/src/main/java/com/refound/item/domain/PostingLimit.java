package com.refound.item.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** BR-09 (7.4): tối đa 10 tin mỗi người trong 24 giờ trượt, tính cả tin đã xóa (xóa rồi đăng lại vẫn bị đếm). */
public final class PostingLimit {

    public static final int MAX_PER_WINDOW = 10;
    public static final Duration WINDOW = Duration.ofHours(24);

    private PostingLimit() {}

    /**
     * @param createdInWindow thời điểm tạo các tin của người này trong 24 giờ qua, thứ tự bất kỳ
     * @return rỗng nếu được đăng; ngược lại thời gian chờ tới khi đủ tin cũ ra khỏi cửa sổ (tối thiểu 1 giây)
     */
    public static Optional<Duration> retryAfter(List<Instant> createdInWindow, Instant now) {
        int n = createdInWindow.size();
        if (n < MAX_PER_WINDOW) {
            return Optional.empty();
        }
        // phải có n − 9 tin ra khỏi cửa sổ để còn chỗ cho tin mới: chờ tới khi tin cũ thứ (n − 10) (đếm từ 0) hết hạn
        Instant oldestToExpire = createdInWindow.stream().sorted().toList().get(n - MAX_PER_WINDOW);
        Duration wait = Duration.between(now, oldestToExpire.plus(WINDOW));
        return Optional.of(wait.compareTo(Duration.ofSeconds(1)) < 0 ? Duration.ofSeconds(1) : wait);
    }
}
