package com.refound.item.domain;

/** Trạng thái tin (05 §5.1). Giá trị DB là tên viết thường, khớp CHECK của {@code items.status}. */
public enum ItemStatus {
    OPEN,
    CLAIMED,
    RETURNED,
    CLOSED,
    EXPIRED,
    UNDER_REVIEW;

    public String db() {
        return name().toLowerCase();
    }

    public static ItemStatus fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
