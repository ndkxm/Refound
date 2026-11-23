package com.refound.claim.domain;

/** Trạng thái yêu cầu nhận (05 §5.2). Giá trị DB là tên viết thường. */
public enum ClaimStatus {
    SUBMITTED,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    COMPLETED,
    DISPUTED;

    public String db() {
        return name().toLowerCase();
    }

    public static ClaimStatus fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
