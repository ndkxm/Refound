package com.refound.matching.domain;

import com.refound.matching.domain.MatchScorer.Components;
import com.refound.matching.domain.MatchScorer.Input;
import java.util.ArrayList;
import java.util.List;

/** Lý do khớp hiển thị cho người dùng (04 §4.2, FR-MATCH-01), hàm thuần. Thứ tự: ảnh, vị trí, thời gian, danh mục. */
public final class MatchReasons {

    static final double STRONG_SIMILARITY = 0.7;
    static final double NEAR = 0.5;
    static final double SOON = 0.7;

    private MatchReasons() {}

    /**
     * @param embeddingDone tin ứng viên đã có vector ({@code embedding_status = done}); chưa thì điểm ảnh/mô tả bằng 0
     *     nên báo người dùng điểm có thể chưa chính xác
     */
    public static List<String> of(Components c, Input raw, boolean embeddingDone) {
        List<String> reasons = new ArrayList<>();
        if (c.v() >= STRONG_SIMILARITY) {
            reasons.add("Ảnh/mô tả rất giống");
        }
        if (c.d() >= 1) {
            reasons.add("Trong khu vực bạn mất");
        } else if (c.d() >= NEAR) {
            long outside = Math.round(raw.distanceM() - (raw.radiusM() == null ? 0 : raw.radiusM()));
            reasons.add("Cách khu vực mất " + outside + " m");
        }
        if (c.t() >= SOON) {
            reasons.add(timeReason(raw.deltaHours()));
        }
        if (c.c() >= 1) {
            reasons.add("Cùng danh mục");
        }
        if (!embeddingDone) {
            reasons.add("Đang xử lý ảnh, điểm có thể chưa chính xác");
        }
        return reasons;
    }

    /**
     * Δh âm (nhặt trước giờ người mất khai, trong sai số 24 h) không ghi "-10 giờ sau": giờ mất vốn chỉ là ước lượng.
     * {@code s_t ≥ 0,7} nghĩa là Δh ≤ τ·ln(1/0,7) ≈ 25,7 h, nên luôn tính bằng giờ.
     */
    static String timeReason(double deltaHours) {
        if (deltaHours <= 0) {
            return "Nhặt gần thời điểm mất";
        }
        if (deltaHours < 1) {
            return "Nhặt chưa tới 1 giờ sau khi mất";
        }
        return "Nhặt " + Math.round(deltaHours) + " giờ sau khi mất";
    }
}
