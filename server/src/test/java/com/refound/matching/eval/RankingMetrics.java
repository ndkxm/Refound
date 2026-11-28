package com.refound.matching.eval;

import java.util.List;
import java.util.Map;

/** top-1, top-5, MRR của một nhóm truy vấn (04 §4.6). Hạng bắt đầu từ 1; 0 là đáp án không có trong danh sách. */
record RankingMetrics(int n, double top1, double top5, double mrr) {

    static RankingMetrics of(List<Integer> ranks) {
        if (ranks.isEmpty()) {
            return new RankingMetrics(0, 0, 0, 0);
        }
        double n = ranks.size();
        return new RankingMetrics(ranks.size(),
                ranks.stream().filter(r -> r == 1).count() / n,
                ranks.stream().filter(r -> r >= 1 && r <= 5).count() / n,
                ranks.stream().mapToDouble(r -> r == 0 ? 0 : 1.0 / r).sum() / n);
    }

    /** Hạng của {@code expected}; ứng viên hòa điểm tính là đứng trước (xếp bi quan, không ăn may thứ tự). */
    static int rank(Map<String, Double> scores, String expected) {
        Double s = scores.get(expected);
        if (s == null) {
            return 0;
        }
        return 1 + (int) scores.entrySet().stream()
                .filter(e -> !e.getKey().equals(expected) && e.getValue() >= s)
                .count();
    }
}
