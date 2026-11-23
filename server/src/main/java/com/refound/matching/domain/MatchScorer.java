package com.refound.matching.domain;

/**
 * Công thức điểm khớp (04 §4.2), hàm thuần: {@code S = w_v·s_v + w_d·s_d + w_t·s_t + w_c·s_c}.
 * Lọc cứng (khác loại, trạng thái, khoảng cách 3 km, thời gian) làm trong SQL trước khi gọi hàm này.
 * Dữ liệu thiếu (null) cho thành phần đó bằng 0, nên không đổi thứ tự giữa các ứng viên cùng thiếu.
 */
public final class MatchScorer {

    private MatchScorer() {}

    /** Danh mục của một phía; {@code group} là {@code categories.group_code}. */
    public record Category(String code, String group) {}

    /**
     * Giá trị thô của một cặp (tin mất L, tin nhặt F) hoặc (câu truy vấn, tin).
     *
     * @param cosine max cosine trên mọi cặp vector của hai phía; null nếu một phía chưa có vector
     * @param distanceM khoảng cách giữa hai vị trí, mét
     * @param radiusM bán kính khu vực mất của phía {@code lost}; null coi là 0
     * @param deltaHours giờ từ lúc mất tới lúc nhặt (âm nếu nhặt trước lúc mất, trong sai số 24 h của lọc cứng)
     */
    public record Input(
            Double cosine, Double distanceM, Integer radiusM, Double deltaHours, Category lost, Category found) {}

    /** Từng thành phần đã chuẩn hóa về [0, 1]; trả cho app dưới tên {@code components: {v, d, t, c}} (06 §6.2). */
    public record Components(double v, double d, double t, double c) {}

    public record Score(double total, Components components) {}

    public static Score score(Input in, MatchWeights w) {
        Components c = new Components(
                similarity(in.cosine(), w),
                distance(in.distanceM(), in.radiusM(), w),
                time(in.deltaHours(), w),
                category(in.lost(), in.found()));
        double total = w.wv() * c.v() + w.wd() * c.d() + w.wt() * c.t() + w.wc() * c.c();
        return new Score(total, c);
    }

    /** {@code clamp((cos − c0) / (c1 − c0), 0, 1)}. */
    static double similarity(Double cosine, MatchWeights w) {
        if (!finite(cosine)) {
            return 0;
        }
        return clamp((cosine - w.c0()) / (w.c1() - w.c0()));
    }

    /** {@code 1} nếu {@code d ≤ r}, ngược lại {@code exp(−(d − r) / λ)}. */
    static double distance(Double distanceM, Integer radiusM, MatchWeights w) {
        if (!finite(distanceM)) {
            return 0;
        }
        double outside = Math.max(distanceM, 0) - (radiusM == null ? 0 : Math.max(radiusM, 0));
        return outside <= 0 ? 1 : Math.exp(-outside / w.lambdaM());
    }

    /** {@code exp(−max(Δh, 0) / τ)}. */
    static double time(Double deltaHours, MatchWeights w) {
        if (!finite(deltaHours)) {
            return 0;
        }
        return Math.exp(-Math.max(deltaHours, 0) / w.tauH());
    }

    /** Cùng mã: 1; cùng nhóm: 0,5; khác hoặc thiếu: 0. */
    static double category(Category lost, Category found) {
        if (lost == null || found == null || lost.code() == null || found.code() == null) {
            return 0;
        }
        if (lost.code().equals(found.code())) {
            return 1;
        }
        return lost.group() != null && lost.group().equals(found.group()) ? 0.5 : 0;
    }

    private static boolean finite(Double x) {
        return x != null && Double.isFinite(x);
    }

    private static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }
}
