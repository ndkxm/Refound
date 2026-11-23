package com.refound.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.refound.matching.domain.MatchScorer.Category;
import com.refound.matching.domain.MatchScorer.Input;
import com.refound.matching.domain.MatchScorer.Score;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Hai ví dụ tính tay ở 04 §4.2 (đáp án tính trước khi viết code) và các ca biên. */
class MatchScorerTest {

    static final MatchWeights W = MatchWeights.DEFAULT;
    static final Category EARPHONES = new Category("earphones", "electronics");
    static final Category PHONE = new Category("phone", "electronics");
    static final Category WALLET = new Category("wallet", "wallet_bag");

    /** Ví dụ 1: tai nghe, nhặt 6 giờ sau, cách tâm 350 m với bán kính 200 m, cos 0,48 → 0,776, lưu và thông báo. */
    @Test
    void example1GoodMatch() {
        Score s = MatchScorer.score(new Input(0.48, 350.0, 200, 6.0, EARPHONES, EARPHONES), W);
        assertThat(s.components().v()).isCloseTo(0.7333, within(1e-4));
        assertThat(s.components().d()).isCloseTo(0.6065, within(1e-4));
        assertThat(s.components().t()).isCloseTo(0.9200, within(1e-4));
        assertThat(s.components().c()).isEqualTo(1);
        assertThat(s.total()).isCloseTo(0.776, within(5e-4));
        assertThat(s.total()).isGreaterThanOrEqualTo(W.notifyThreshold());
    }

    /** Ví dụ 2: nhặt trước lúc mất 10 giờ, khác nhóm, d = 1500, r = 300, cos 0,30 → 0,320, không lưu. */
    @Test
    void example2NoMatch() {
        Score s = MatchScorer.score(new Input(0.30, 1500.0, 300, -10.0, EARPHONES, WALLET), W);
        assertThat(s.components().v()).isCloseTo(0.3333, within(1e-4));
        assertThat(s.components().d()).isCloseTo(0.0183, within(1e-4));
        assertThat(s.components().t()).isEqualTo(1);
        assertThat(s.components().c()).isEqualTo(0);
        assertThat(s.total()).isCloseTo(0.320, within(5e-4));
        assertThat(s.total()).isLessThan(W.storeThreshold());
    }

    @ParameterizedTest(name = "cos {0} → s_v {1}")
    @CsvSource({"-0.2, 0", "0.0, 0", "0.15, 0", "0.375, 0.5", "0.60, 1", "0.95, 1"})
    void similarityClampsBetweenC0AndC1(double cos, double expected) {
        assertThat(MatchScorer.similarity(cos, W)).isCloseTo(expected, within(1e-12));
    }

    @Test
    void distanceInsideRadiusIsOne() {
        assertThat(MatchScorer.distance(200.0, 200, W)).isEqualTo(1); // d = r
        assertThat(MatchScorer.distance(0.0, 200, W)).isEqualTo(1);
        assertThat(MatchScorer.distance(0.0, null, W)).isEqualTo(1);
        // λ = 300: ra ngoài 300 m còn e⁻¹
        assertThat(MatchScorer.distance(500.0, 200, W)).isCloseTo(Math.exp(-1), within(1e-12));
        assertThat(MatchScorer.distance(300.0, null, W)).isCloseTo(Math.exp(-1), within(1e-12));
    }

    @Test
    void timeIsOneAtOrBeforeLossAndDecays() {
        assertThat(MatchScorer.time(0.0, W)).isEqualTo(1); // Δh = 0
        assertThat(MatchScorer.time(-23.0, W)).isEqualTo(1);
        assertThat(MatchScorer.time(72.0, W)).isCloseTo(Math.exp(-1), within(1e-12)); // τ = 72 h
    }

    @Test
    void categoryExactGroupOrNothing() {
        assertThat(MatchScorer.category(EARPHONES, EARPHONES)).isEqualTo(1);
        assertThat(MatchScorer.category(EARPHONES, PHONE)).isEqualTo(0.5);
        assertThat(MatchScorer.category(EARPHONES, WALLET)).isEqualTo(0);
        assertThat(MatchScorer.category(null, WALLET)).isEqualTo(0); // tìm kiếm không chọn danh mục
        assertThat(MatchScorer.category(new Category("x", null), new Category("y", null))).isEqualTo(0);
    }

    /** Thiếu dữ liệu: thành phần đó bằng 0, không ném (embedding chưa xong, tìm kiếm không có vị trí/thời gian). */
    @Test
    void missingInputsScoreZero() {
        Score s = MatchScorer.score(new Input(null, null, null, null, null, null), W);
        assertThat(s.total()).isZero();
        Score nan = MatchScorer.score(new Input(Double.NaN, Double.NaN, 100, Double.POSITIVE_INFINITY, null, null), W);
        assertThat(nan.total()).isZero();
    }

    @Test
    void perfectMatchIsOne() {
        assertThat(MatchScorer.score(new Input(0.9, 10.0, 100, 1e-9, WALLET, WALLET), W).total())
                .isCloseTo(1, within(1e-9));
    }

    @Test
    void invalidWeightsFailFast() {
        assertThatThrownBy(() -> new MatchWeights(0.5, 0.2, 0.15, 0.2, 0.15, 0.6, 300, 72, 0.45, 0.65))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tổng trọng số");
        assertThatThrownBy(() -> new MatchWeights(0.5, 0.2, 0.15, 0.15, 0.6, 0.6, 300, 72, 0.45, 0.65))
                .hasMessageContaining("c0 < c1");
        assertThatThrownBy(() -> new MatchWeights(0.5, 0.2, 0.15, 0.15, 0.15, 0.6, 0, 72, 0.45, 0.65))
                .hasMessageContaining("λ");
        assertThatThrownBy(() -> new MatchWeights(1.2, -0.2, 0, 0, 0.15, 0.6, 300, 72, 0.45, 0.65))
                .hasMessageContaining("[0, 1]");
        assertThatThrownBy(() -> new MatchWeights(0.5, 0.2, 0.15, 0.15, 0.15, 0.6, 300, 72, 0.7, 0.65))
                .hasMessageContaining("ngưỡng");
    }
}
