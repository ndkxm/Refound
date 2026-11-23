package com.refound.claim.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Bảng 05 §5.3, gồm các biên −1/0, 29/30. */
class AttemptPolicyTest {

    @ParameterizedTest(name = "uy tín {0} → tối đa {1}")
    @CsvSource({"-100, 1", "-1, 1", "0, 3", "29, 3", "30, 5", "1000, 5"})
    void maxAttempts(int reputation, int max) {
        assertThat(AttemptPolicy.maxAttempts(reputation)).isEqualTo(max);
    }

    @ParameterizedTest(name = "uy tín {0}, đã bị từ chối {1} lần → gửi được: {2}")
    @CsvSource({"-1, 0, true", "-1, 1, false", "0, 2, true", "0, 3, false", "30, 4, true", "30, 5, false"})
    void canSubmit(int reputation, int rejected, boolean allowed) {
        assertThat(AttemptPolicy.canSubmit(reputation, rejected)).isEqualTo(allowed);
    }
}
