package com.refound.matching.infrastructure;

import java.util.List;

/** Tạo vector 768 chiều cho chữ và ảnh trong cùng một không gian (ADR-02). Test dùng bản giả (09 §9.2). */
public interface EmbeddingClient {

    int DIMENSIONS = 768;

    /** Tên model, ghi vào cột {@code model} để không trộn vector của hai model (7.3). */
    String model();

    /**
     * Một vector cho mỗi phần đầu vào, cùng thứ tự.
     *
     * @throws EmbeddingException lỗi mạng, lỗi dịch vụ, hết hạn mức hoặc trả sai số vector
     */
    List<float[]> embed(List<Input> inputs);

    /** Một phần đầu vào: chữ đã có tiền tố ({@code EmbeddingText}) hoặc ảnh JPEG 512px. */
    sealed interface Input {

        record Text(String text) implements Input {}

        record Image(byte[] jpeg) implements Input {}
    }

    class EmbeddingException extends RuntimeException {

        private final boolean rateLimited;

        public EmbeddingException(String message, boolean rateLimited, Throwable cause) {
            super(message, cause);
            this.rateLimited = rateLimited;
        }

        /** HTTP 429: hết hạn mức theo phút hoặc ngày, cần tạm dừng mọi lời gọi (7.3). */
        public boolean rateLimited() {
            return rateLimited;
        }
    }
}
