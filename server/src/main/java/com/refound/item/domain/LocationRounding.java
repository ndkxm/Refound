package com.refound.item.domain;

import com.refound.common.GeoPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Vị trí công khai (02 §2.3, BR-11, BR-13), tính một lần khi ghi tin.
 * Tin {@code found}: làm tròn 3 chữ số thập phân (ô ~110 m, mức tòa nhà), nửa lên. Tin {@code lost}: vốn là khu vực
 * (tâm + bán kính), công khai nguyên giá trị.
 */
public final class LocationRounding {

    private static final int DECIMALS = 3;

    private LocationRounding() {}

    public static GeoPoint publicLocation(ItemType type, GeoPoint exact) {
        return type == ItemType.FOUND ? new GeoPoint(round(exact.lat()), round(exact.lng())) : exact;
    }

    /**
     * {@code BigDecimal.valueOf} đi qua chuỗi thập phân ngắn nhất của double, nên 10.8705 làm tròn thành 10.871;
     * {@code new BigDecimal(10.8705)} sẽ thấy 10.87049999… và cho 10.870 (sai ví dụ 2.3).
     */
    static double round(double degrees) {
        return BigDecimal.valueOf(degrees).setScale(DECIMALS, RoundingMode.HALF_UP).doubleValue();
    }
}
