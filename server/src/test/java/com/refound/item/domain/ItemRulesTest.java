package com.refound.item.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.refound.common.ApiException.FieldError;
import com.refound.common.GeoPoint;
import com.refound.item.domain.ItemRules.Draft;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Luật dữ liệu tin (I1, BR-08, BR-13, ADR-14). */
class ItemRulesTest {

    static final Instant NOW = Instant.parse("2026-11-23T05:00:00Z");
    static final GeoPoint HCM = new GeoPoint(10.87, 106.80);

    static Draft found() {
        return new Draft(ItemType.FOUND, "wallet", "Ví da nâu", NOW.minus(Duration.ofHours(1)), HCM, null,
                "Trong ví có gì?", "Thẻ xe");
    }

    static Draft lost() {
        return new Draft(ItemType.LOST, "earphones", "Tai nghe trắng", NOW.minus(Duration.ofHours(1)), HCM, 200,
                null, null);
    }

    @Test
    void validFoundAndLost() {
        assertThat(ItemRules.validate(found(), true, false, 1, NOW)).isEmpty();
        assertThat(ItemRules.validate(found(), true, false, 4, NOW)).isEmpty();
        assertThat(ItemRules.validate(lost(), true, false, 0, NOW)).isEmpty();
        assertThat(ItemRules.validate(lost(), true, true, 0, NOW)).isEmpty(); // tin mất thẻ, không ảnh
    }

    @Test
    void everythingMissingReportsEveryField() {
        Draft empty = new Draft(null, null, null, null, null, null, null, null);
        assertThat(fields(ItemRules.validate(empty, false, false, 0, NOW)))
                .containsExactly("type", "category", "description", "happenedAt", "location");
    }

    @Test
    void foundNeedsImageAndSecretAndNoRadius() {
        Draft d = new Draft(ItemType.FOUND, "wallet", "Ví da nâu", NOW, HCM, 100, null, null);
        assertThat(fields(ItemRules.validate(d, true, false, 0, NOW)))
                .containsExactly("radiusM", "full", "question", "answer");
    }

    @Test
    void lostRules() {
        Draft withSecret = new Draft(ItemType.LOST, "wallet", "Ví da nâu", NOW, HCM, 200, "Hỏi gì đó?", "x");
        assertThat(fields(ItemRules.validate(withSecret, true, false, 0, NOW))).containsExactly("question");
        // BR-08: tin mất danh mục nhạy cảm không kèm ảnh
        assertThat(fields(ItemRules.validate(lost(), true, true, 1, NOW))).containsExactly("full");
        assertThat(fields(ItemRules.validate(lost(), true, false, 5, NOW))).containsExactly("full");
    }

    @ParameterizedTest(name = "bán kính {0} hợp lệ: {1}")
    @CsvSource({"49, false", "50, true", "2000, true", "2001, false", ", false"})
    void lostRadius(Integer radius, boolean ok) {
        Draft d = new Draft(ItemType.LOST, "wallet", "Ví da nâu", NOW, HCM, radius, null, null);
        assertThat(ItemRules.validate(d, true, false, 0, NOW).isEmpty()).isEqualTo(ok);
    }

    /** Đếm theo code point như char_length: 4 emoji là 4 ký tự (8 đơn vị UTF-16) nên vẫn dưới 5. */
    @Test
    void lengthsCountCodePoints() {
        Draft emoji = new Draft(ItemType.LOST, "wallet", "😀😀😀😀", NOW, HCM, 200, null, null);
        assertThat(fields(ItemRules.validate(emoji, true, false, 0, NOW))).containsExactly("description");
        Draft longOk = new Draft(ItemType.LOST, "wallet", "😀".repeat(1000), NOW, HCM, 200, null, null);
        assertThat(ItemRules.validate(longOk, true, false, 0, NOW)).isEmpty();
        Draft tooLong = new Draft(ItemType.LOST, "wallet", "a".repeat(1001), NOW, HCM, 200, null, null);
        assertThat(fields(ItemRules.validate(tooLong, true, false, 0, NOW))).containsExactly("description");
    }

    @Test
    void happenedAtMayBeSlightlyAheadButNotFuture() {
        Draft skew = new Draft(ItemType.LOST, "wallet", "Ví da nâu", NOW.plus(Duration.ofMinutes(9)), HCM, 200,
                null, null);
        assertThat(ItemRules.validate(skew, true, false, 0, NOW)).isEmpty();
        Draft future = new Draft(ItemType.LOST, "wallet", "Ví da nâu", NOW.plus(Duration.ofHours(1)), HCM, 200,
                null, null);
        assertThat(fields(ItemRules.validate(future, true, false, 0, NOW))).containsExactly("happenedAt");
    }

    @ParameterizedTest(name = "({0}, {1})")
    @CsvSource({"91, 0", "-91, 0", "0, 181", "0, -180.5", "NaN, 0"})
    void locationOutOfRange(double lat, double lng) {
        Draft d = new Draft(ItemType.LOST, "wallet", "Ví da nâu", NOW, new GeoPoint(lat, lng), 200, null, null);
        assertThat(fields(ItemRules.validate(d, true, false, 0, NOW))).containsExactly("location");
    }

    @Test
    void cleanStripsBlankAndNul() {
        assertThat(ItemRules.clean("  ví  ")).isEqualTo("ví");
        assertThat(ItemRules.clean("   ")).isNull();
        assertThat(ItemRules.clean(null)).isNull();
        assertThat(ItemRules.clean("a\u0000b")).isEqualTo("ab");
    }

    private static List<String> fields(List<FieldError> errors) {
        return errors.stream().map(FieldError::field).toList();
    }
}
