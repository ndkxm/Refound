package com.refound.matching.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tìm kiếm hạ cấp khi Gemini lỗi (04 §4.3): lọc {@code description ILIKE '%từ khóa%'}. Cả câu như "tai nghe trắng
 * hộp sứt góc" hầu như không bao giờ xuất hiện nguyên văn, nên tách thành từ và giữ tin chứa ít nhất một từ.
 */
public final class KeywordFilter {

    static final int MAX_WORDS = 8;
    static final int MIN_LENGTH = 2;

    private KeywordFilter() {}

    /** Mẫu ILIKE đã thoát {@code \ % _}, dùng với {@code ESCAPE '\'}; rỗng nếu không còn từ nào đủ dài. */
    public static List<String> patterns(String text) {
        if (text == null) {
            return List.of();
        }
        Set<String> words = new LinkedHashSet<>();
        for (String w : text.toLowerCase(Locale.ROOT).split("[\\s\\p{Punct}]+")) {
            if (w.codePointCount(0, w.length()) >= MIN_LENGTH && words.size() < MAX_WORDS) {
                words.add(w);
            }
        }
        List<String> out = new ArrayList<>();
        for (String w : words) {
            out.add("%" + w.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        return out;
    }
}
