package com.refound.item.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** BR-09: 10 tin / 24 giờ trượt. */
class PostingLimitTest {

    static final Instant NOW = Instant.parse("2026-11-23T05:00:00Z");

    @Test
    void underLimitIsAllowed() {
        assertThat(PostingLimit.retryAfter(hoursAgo(9), NOW)).isEmpty();
        assertThat(PostingLimit.retryAfter(List.of(), NOW)).isEmpty();
    }

    /** 10 tin tạo 1, 2, …, 10 giờ trước: tin cũ nhất (10 giờ trước) ra khỏi cửa sổ sau 14 giờ nữa. */
    @Test
    void atLimitWaitsForOldest() {
        List<Instant> created = hoursAgo(10);
        Collections.shuffle(created);
        assertThat(PostingLimit.retryAfter(created, NOW)).contains(Duration.ofHours(14));
    }

    /** 11 tin (hai request song song lọt qua trước khi khóa): phải chờ 2 tin cũ nhất ra, tức tin 10 giờ trước. */
    @Test
    void overLimitWaitsForEnoughToExpire() {
        List<Instant> created = hoursAgo(11); // 1..11 giờ trước
        assertThat(PostingLimit.retryAfter(created, NOW)).contains(Duration.ofHours(14));
    }

    @Test
    void waitIsAtLeastOneSecond() {
        List<Instant> created = new ArrayList<>(hoursAgo(9));
        created.add(NOW.minus(PostingLimit.WINDOW)); // vừa đúng biên
        assertThat(PostingLimit.retryAfter(created, NOW)).contains(Duration.ofSeconds(1));
    }

    private static List<Instant> hoursAgo(int n) {
        List<Instant> list = new ArrayList<>();
        for (int h = 1; h <= n; h++) {
            list.add(NOW.minus(Duration.ofHours(h)));
        }
        return list;
    }
}
