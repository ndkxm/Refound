package com.refound.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KeywordFilterTest {

    @Test
    void splitsIntoWordsDropsShortAndDuplicates() {
        assertThat(KeywordFilter.patterns("Tai nghe trắng, hộp sứt góc. Tai"))
                .containsExactly("%tai%", "%nghe%", "%trắng%", "%hộp%", "%sứt%", "%góc%");
        assertThat(KeywordFilter.patterns("a b")).isEmpty();
        assertThat(KeywordFilter.patterns(null)).isEmpty();
    }

    @Test
    void wildcardsCannotBeInjected() {
        assertThat(KeywordFilter.patterns("100% ví_da")).containsExactly("%100%", "%ví%", "%da%");
    }

    @Test
    void atMostEightWords() {
        assertThat(KeywordFilter.patterns("một hai ba bốn năm sáu bảy tám chín mười")).hasSize(8);
    }
}
