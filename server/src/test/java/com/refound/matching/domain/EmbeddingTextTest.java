package com.refound.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tiền tố của {@code gemini-embedding-2} (04 §4.1, ADR-02). */
class EmbeddingTextTest {

    @Test
    void itemTextWithAndWithoutCaption() {
        assertThat(EmbeddingText.forItem("Tai nghe", "tai nghe trắng hộp sứt góc", null))
                .isEqualTo("title: Tai nghe | text: tai nghe trắng hộp sứt góc");
        assertThat(EmbeddingText.forItem("Tai nghe", "tai nghe trắng", "Tai nghe không dây màu trắng"))
                .isEqualTo("title: Tai nghe | text: tai nghe trắng. Tai nghe không dây màu trắng");
        // mô tả đã có dấu câu cuối thì không thêm dấu chấm thứ hai
        assertThat(EmbeddingText.forItem("Ví", "Ví nâu.", "Ví da")).isEqualTo("title: Ví | text: Ví nâu. Ví da");
        assertThat(EmbeddingText.forItem("Ví", "Ví nâu", "  ")).isEqualTo("title: Ví | text: Ví nâu");
    }

    @Test
    void queryText() {
        assertThat(EmbeddingText.forQuery("  tai nghe trắng ")).isEqualTo("task: search result | query: tai nghe trắng");
    }
}
