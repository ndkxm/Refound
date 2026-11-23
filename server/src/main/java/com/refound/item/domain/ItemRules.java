package com.refound.item.domain;

import com.refound.common.ApiException.FieldError;
import com.refound.common.GeoPoint;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Luật dữ liệu của một tin (I1, BR-13, ADR-14), hàm thuần, dùng cho cả tạo và sửa. Trả mọi lỗi một lượt để app
 * hiện cạnh từng ô. Độ dài đếm theo code point như {@code char_length} của Postgres (emoji là 1 ký tự), để không có
 * chuỗi nào qua được đây mà vẫn vi phạm CHECK của DB.
 */
public final class ItemRules {

    public static final int MAX_IMAGES = 4;
    /** Đồng hồ máy người dùng có thể nhanh hơn server vài phút. */
    static final Duration CLOCK_SKEW = Duration.ofMinutes(10);

    private ItemRules() {}

    /** Nội dung một tin sau khi đã bỏ khoảng trắng đầu cuối. {@code type}, {@code category} null nghĩa là thiếu/sai. */
    public record Draft(
            ItemType type,
            String category,
            String description,
            Instant happenedAt,
            GeoPoint location,
            Integer radiusM,
            String question,
            String answer) {}

    /**
     * @param categoryExists {@code category} có trong bảng {@code categories}
     * @param sensitive danh mục nhạy cảm (BR-08)
     * @param imageCount số ảnh của tin (sau khi tạo/sửa)
     */
    public static List<FieldError> validate(
            Draft d, boolean categoryExists, boolean sensitive, int imageCount, Instant now) {
        List<FieldError> errors = new ArrayList<>();
        if (d.type() == null) {
            errors.add(new FieldError("type", "Loại tin phải là lost hoặc found"));
        }
        if (d.category() == null || !categoryExists) {
            errors.add(new FieldError("category", "Danh mục không hợp lệ"));
        }
        length(errors, "description", d.description(), 5, 1000, "Mô tả dài 5–1000 ký tự");
        if (d.happenedAt() == null) {
            errors.add(new FieldError("happenedAt", "Thiếu thời điểm"));
        } else if (d.happenedAt().isAfter(now.plus(CLOCK_SKEW))) {
            errors.add(new FieldError("happenedAt", "Thời điểm không được ở tương lai"));
        }
        if (d.location() == null || !d.location().isValid()) {
            errors.add(new FieldError("location", "Vị trí không hợp lệ"));
        }
        if (imageCount > MAX_IMAGES) {
            errors.add(new FieldError("full", "Tối đa " + MAX_IMAGES + " ảnh"));
        }
        if (d.type() == ItemType.LOST) {
            if (d.radiusM() == null || d.radiusM() < 50 || d.radiusM() > 2000) {
                errors.add(new FieldError("radiusM", "Bán kính khu vực mất phải từ 50 đến 2000 m"));
            }
            if (d.question() != null || d.answer() != null) { // BR-13: tin mất công khai, không có câu hỏi kín
                errors.add(new FieldError("question", "Tin mất không có câu hỏi xác minh"));
            }
            if (sensitive && imageCount > 0) { // BR-08, ADR-14
                errors.add(new FieldError("full", "Tin mất giấy tờ, thẻ không được kèm ảnh"));
            }
        } else if (d.type() == ItemType.FOUND) {
            if (d.radiusM() != null) {
                errors.add(new FieldError("radiusM", "Tin nhặt được không có bán kính"));
            }
            if (imageCount < 1) {
                errors.add(new FieldError("full", "Tin nhặt được cần ít nhất 1 ảnh"));
            }
            length(errors, "question", d.question(), 5, 200, "Câu hỏi xác minh dài 5–200 ký tự");
            length(errors, "answer", d.answer(), 1, 200, "Đáp án dài 1–200 ký tự");
        }
        return errors;
    }

    /** Bỏ khoảng trắng đầu cuối và ký tự NUL (Postgres từ chối U+0000 trong text); chuỗi rỗng coi như không gửi. */
    public static String clean(String s) {
        if (s == null) {
            return null;
        }
        String stripped = s.replace("\u0000", "").strip();
        return stripped.isEmpty() ? null : stripped;
    }

    /** Cho {@code PATCH}: null là không sửa; có gửi thì bỏ NUL và khoảng trắng đầu cuối, rỗng vẫn giữ để báo lỗi độ dài. */
    public static String normalize(String s) {
        return s == null ? null : s.replace("\u0000", "").strip();
    }

    private static void length(List<FieldError> errors, String field, String value, int min, int max, String message) {
        int n = value == null ? 0 : value.codePointCount(0, value.length());
        if (n < min || n > max) {
            errors.add(new FieldError(field, message));
        }
    }
}
