package com.refound.item.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.refound.common.GeoPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Ví dụ tính tay ở 02 §2.3 (đáp án tính trước khi viết code). */
class LocationRoundingTest {

    @Test
    void example23() {
        assertThat(LocationRounding.publicLocation(ItemType.FOUND, new GeoPoint(10.870345, 106.803128)))
                .isEqualTo(new GeoPoint(10.870, 106.803));
        // nửa lên: .8705 → .871, .8035 → .804 (new BigDecimal(double) sẽ ra .870 vì 10.8705 là 10.87049999… nhị phân)
        assertThat(LocationRounding.publicLocation(ItemType.FOUND, new GeoPoint(10.8705, 106.8035)))
                .isEqualTo(new GeoPoint(10.871, 106.804));
    }

    @Test
    void lostIsAlreadyAnAreaAndStaysExact() {
        GeoPoint exact = new GeoPoint(10.870345, 106.803128);
        assertThat(LocationRounding.publicLocation(ItemType.LOST, exact)).isSameAs(exact);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"-10.8705, -10.871", "0.0004, 0.0", "179.9996, 180.0", "-33.12345, -33.123", "10.87, 10.87"})
    void roundsHalfAwayFromZeroAndStaysInRange(double in, double out) {
        assertThat(LocationRounding.round(in)).isEqualTo(out);
    }
}
