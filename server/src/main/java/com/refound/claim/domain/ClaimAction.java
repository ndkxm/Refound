package com.refound.claim.domain;

/** Hành động trên claim, mỗi giá trị ứng với một dòng của bảng 05 §5.2 (mã C… trong chú thích). */
public enum ClaimAction {
    /** C2: Finder chấp nhận. */
    ACCEPT,
    /** C3: Finder từ chối (lý do), được đếm cho BR-05. */
    REJECT,
    /** C4: Owner rút. */
    WITHDRAW,
    /** C4b khi tin hết hạn (I8): claim {@code submitted} → {@code cancelled}. */
    AUTO_CANCEL,
    /**
     * C4b khi tin bị đóng hoặc gỡ (I7, I12), và các claim {@code submitted} khác khi một claim bàn giao xong (C6):
     * → {@code rejected}. Tách khỏi {@link #AUTO_CANCEL} vì bảng 5.1 ghi hai đích khác nhau.
     */
    AUTO_REJECT,
    /** C5: một bên hủy sau khi đã chấp nhận. */
    CANCEL,
    /** C5b: Finder tạo mã QR mới, trạng thái giữ nguyên. */
    GENERATE_CODE,
    /** C6: Owner quét QR hợp lệ. */
    SCAN_QR,
    /** C6b: Finder tự xác nhận kèm lý do. */
    MANUAL_COMPLETE,
    /** C7: một bên báo "không phải chủ". */
    DISPUTE,
    /** C8: admin xử lý tranh chấp. */
    RESOLVE,
    /** C9: 7 ngày không có hành động. */
    EXPIRE,
    /** C10: một bên chặn bên kia (R03). */
    BLOCK
}
