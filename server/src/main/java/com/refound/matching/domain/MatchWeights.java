package com.refound.matching.domain;

/**
 * Tham số của công thức điểm khớp (04 §4.2). Giá trị khởi đầu ở {@link #DEFAULT}; hiệu chỉnh ở H01 bằng bộ mẫu P0-13.
 * Kiểm tra ngay khi tạo, và bean ở {@code MatchingConfig} tạo lúc khởi động, nên cấu hình sai làm server không lên
 * thay vì chấm điểm sai âm thầm.
 *
 * @param lambdaM λ, mét: ra ngoài bán kính λ mét thì {@code s_d} còn e⁻¹ ≈ 0,37
 * @param tauH τ, giờ: nhặt sau τ giờ thì {@code s_t} còn e⁻¹ ≈ 0,37
 */
public record MatchWeights(
        double wv,
        double wd,
        double wt,
        double wc,
        double c0,
        double c1,
        double lambdaM,
        double tauH,
        double storeThreshold,
        double notifyThreshold) {

    public static final MatchWeights DEFAULT = new MatchWeights(0.50, 0.20, 0.15, 0.15, 0.15, 0.60, 300, 72, 0.45, 0.65);

    private static final double EPSILON = 1e-9;

    public MatchWeights {
        for (double w : new double[] {wv, wd, wt, wc}) {
            require(w >= 0 && w <= 1, "mỗi trọng số phải trong [0, 1]");
        }
        require(Math.abs(wv + wd + wt + wc - 1) < EPSILON, "tổng trọng số phải bằng 1");
        require(c0 >= 0 && c0 < c1 && c1 <= 1, "cần 0 ≤ c0 < c1 ≤ 1");
        require(lambdaM > 0 && tauH > 0, "λ và τ phải dương");
        require(storeThreshold >= 0 && storeThreshold <= notifyThreshold && notifyThreshold <= 1,
                "cần 0 ≤ ngưỡng lưu ≤ ngưỡng thông báo ≤ 1");
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new IllegalArgumentException("MatchWeights sai: " + message);
        }
    }
}
