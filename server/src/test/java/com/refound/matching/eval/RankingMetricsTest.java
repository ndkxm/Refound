package com.refound.matching.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RankingMetricsTest {

    @Test
    void rankCountsTiesAgainstExpected() {
        // f2 hòa điểm với đáp án f3: xếp bi quan, f3 đứng thứ 3
        assertThat(RankingMetrics.rank(Map.of("f1", 0.9, "f2", 0.8, "f3", 0.8), "f3")).isEqualTo(3);
        assertThat(RankingMetrics.rank(Map.of("f1", 0.9, "f2", 0.8), "f1")).isEqualTo(1);
    }

    @Test
    void rankZeroWhenExpectedMissing() {
        assertThat(RankingMetrics.rank(Map.of("f1", 0.9), "f9")).isZero();
    }

    @Test
    void summaryByHand() {
        // hạng 1, 3, 6, không có: top-1 = 1/4; top-5 = 2/4; MRR = (1 + 1/3 + 1/6 + 0) / 4 = 0,375
        RankingMetrics m = RankingMetrics.of(List.of(1, 3, 6, 0));
        assertThat(m.n()).isEqualTo(4);
        assertThat(m.top1()).isCloseTo(0.25, within(1e-9));
        assertThat(m.top5()).isCloseTo(0.5, within(1e-9));
        assertThat(m.mrr()).isCloseTo(0.375, within(1e-9));
    }

    @Test
    void emptyIsZero() {
        RankingMetrics m = RankingMetrics.of(List.of());
        assertThat(m.n()).isZero();
        assertThat(m.top5()).isZero();
    }
}
