package com.refound.item.domain;

/** BR-01: một tin có đúng một loại. Giá trị DB là tên viết thường. */
public enum ItemType {
    LOST,
    FOUND;

    public String db() {
        return name().toLowerCase();
    }

    public static ItemType fromDb(String value) {
        return valueOf(value.toUpperCase());
    }
}
