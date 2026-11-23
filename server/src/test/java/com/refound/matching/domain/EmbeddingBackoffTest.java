package com.refound.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 04 §4.1: thử lại sau 1, 2, 4, 8, 16, 32 phút rồi {@code failed}. */
class EmbeddingBackoffTest {

    @ParameterizedTest(name = "lỗi lần {0} → chờ {1} phút")
    @CsvSource({"1, 1", "2, 2", "3, 4", "4, 8", "5, 16", "6, 32"})
    void doublesEachTime(int failures, long minutes) {
        assertThat(EmbeddingBackoff.retryDelay(failures)).contains(Duration.ofMinutes(minutes));
    }

    @ParameterizedTest(name = "lỗi lần {0} → failed")
    @CsvSource({"7", "8", "100"})
    void givesUpAfterSixRetries(int failures) {
        assertThat(EmbeddingBackoff.retryDelay(failures)).isEmpty();
    }
}
