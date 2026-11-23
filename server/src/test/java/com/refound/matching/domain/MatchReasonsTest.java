package com.refound.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.refound.matching.domain.MatchScorer.Category;
import com.refound.matching.domain.MatchScorer.Input;
import com.refound.matching.domain.MatchScorer.Score;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Bảng "Lý do khớp" ở 04 §4.2. */
class MatchReasonsTest {

    static final Category EARPHONES = new Category("earphones", "electronics");

    /** Ví dụ 1 của 4.2: s_v 0,73, s_d 0,61 (ngoài 150 m), s_t 0,92 (6 giờ), cùng danh mục. */
    @Test
    void example1HasAllReasons() {
        Input in = new Input(0.48, 350.0, 200, 6.0, EARPHONES, EARPHONES);
        assertThat(reasons(in, true)).containsExactly(
                "Ảnh/mô tả rất giống", "Cách khu vực mất 150 m", "Nhặt 6 giờ sau khi mất", "Cùng danh mục");
    }

    @Test
    void insideAreaAndProcessing() {
        Input in = new Input(null, 120.0, 200, 30.0, EARPHONES, new Category("phone", "electronics"));
        // s_v = 0 (chưa có vector), s_t = exp(−30/72) = 0,66 < 0,7, cùng nhóm không phải cùng mã
        assertThat(reasons(in, false))
                .containsExactly("Trong khu vực bạn mất", "Đang xử lý ảnh, điểm có thể chưa chính xác");
    }

    @Test
    void farAndWeakHasNoReason() {
        // ngoài 300 m: s_d = e⁻¹ ≈ 0,37 < 0,5
        assertThat(reasons(new Input(0.2, 500.0, 200, null, null, null), true)).isEmpty();
    }

    @ParameterizedTest(name = "Δh {0} → {1}")
    @CsvSource({
        "-10,  Nhặt gần thời điểm mất",
        "0,    Nhặt gần thời điểm mất",
        "0.4,  Nhặt chưa tới 1 giờ sau khi mất",
        "1,    Nhặt 1 giờ sau khi mất",
        "5.6,  Nhặt 6 giờ sau khi mất",
    })
    void timeReason(double deltaHours, String text) {
        assertThat(MatchReasons.timeReason(deltaHours)).isEqualTo(text);
    }

    private static List<String> reasons(Input in, boolean embeddingDone) {
        Score s = MatchScorer.score(in, MatchWeights.DEFAULT);
        return MatchReasons.of(s.components(), in, embeddingDone);
    }
}
