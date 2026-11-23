package com.refound.matching.domain;

/**
 * Chữ gửi đi embed (04 §4.1, ADR-02). {@code gemini-embedding-2} không nhận {@code task_type}, nên loại tác vụ ghi
 * trong tiền tố: tài liệu dùng {@code title: … | text: …}, câu tìm kiếm dùng {@code task: search result | query: …}.
 */
public final class EmbeddingText {

    private EmbeddingText() {}

    /** {@code "title: {danh mục} | text: {mô tả}. {mô tả ảnh}"}; bỏ phần mô tả ảnh nếu chưa có. */
    public static String forItem(String categoryName, String description, String aiCaption) {
        StringBuilder s = new StringBuilder("title: ").append(categoryName).append(" | text: ").append(description.strip());
        if (aiCaption != null && !aiCaption.isBlank()) {
            char last = s.charAt(s.length() - 1);
            s.append(".!?…".indexOf(last) >= 0 ? " " : ". ").append(aiCaption.strip());
        }
        return s.toString();
    }

    public static String forQuery(String text) {
        return "task: search result | query: " + text.strip();
    }
}
