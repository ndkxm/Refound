package com.refound.common;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Thời điểm trong DTO luôn mang múi giờ Việt Nam ({@code +07:00}, BR-12, 06 §6.1); JDBC đọc/ghi bằng UTC. */
public final class Times {

    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private Times() {}

    public static OffsetDateTime vn(Instant t) {
        return t == null ? null : t.atZone(VN).toOffsetDateTime();
    }

    /** pgjdbc ghi {@code timestamptz} từ {@link OffsetDateTime} (không nhận {@link Instant} trực tiếp). */
    public static OffsetDateTime utc(Instant t) {
        return t == null ? null : t.atOffset(ZoneOffset.UTC);
    }
}
