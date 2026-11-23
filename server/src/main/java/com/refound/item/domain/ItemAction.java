package com.refound.item.domain;

/** Hành động trên tin, mỗi giá trị ứng với một dòng của bảng 05 §5.1 (mã I… trong chú thích). */
public enum ItemAction {
    /** I2: tác giả sửa. */
    EDIT,
    /** I3: claim được chấp nhận (C2). */
    CLAIM_ACCEPTED,
    /** I4: claim bị hủy, tranh chấp, hết hạn, chặn (C5, C7, C9, C10). */
    CLAIM_RELEASED,
    /** I5: bàn giao xong (C6, C6b); cũng dùng cho tin {@code lost} liên kết với claim đó. */
    HANDOVER_DONE,
    /** I6: chủ tin {@code lost} tự báo đã tìm thấy. */
    MARK_FOUND,
    /** I7: tác giả đóng tin. */
    CLOSE,
    /** I8: 30 ngày không hoạt động. */
    EXPIRE,
    /** I8a: báo sắp hết hạn, không đổi trạng thái. */
    WARN_EXPIRY,
    /** I9: tác giả gia hạn. */
    RENEW,
    /** I10: đủ 3 báo cáo. */
    REPORT_THRESHOLD,
    /** I11: admin khôi phục. */
    RESTORE,
    /** I12: admin gỡ. */
    REMOVE,
    /** I13: tác giả xóa mềm ({@code deleted_at}), trạng thái giữ nguyên. */
    DELETE
}
