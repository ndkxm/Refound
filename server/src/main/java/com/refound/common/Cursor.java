package com.refound.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Con trỏ trang (06 §6.1) cho danh sách sắp theo {@code (thời điểm desc, id desc)}: mã hóa cả khóa sắp xếp, không chỉ
 * id, nên tin ở mốc con trỏ bị xóa giữa hai lần tải trang vẫn phân trang đúng. App coi như chuỗi mờ.
 */
public record Cursor(Instant at, UUID id) {

    public String encode() {
        long micros = ChronoUnit.MICROS.between(Instant.EPOCH, at); // timestamptz của Postgres chính xác tới micro giây
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((micros + "_" + id).getBytes(StandardCharsets.US_ASCII));
    }

    /** @return null nếu {@code value} rỗng (trang đầu) */
    public static Cursor decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.US_ASCII);
            int sep = raw.indexOf('_');
            long micros = Long.parseLong(raw.substring(0, sep));
            return new Cursor(Instant.EPOCH.plus(micros, ChronoUnit.MICROS), UUID.fromString(raw.substring(sep + 1)));
        } catch (RuntimeException e) {
            throw ApiException.validation("Con trỏ trang không hợp lệ.");
        }
    }
}
