package com.refound.item.domain;

import com.refound.common.ApiException;
import com.refound.common.ApiException.FieldError;
import java.util.List;

/** Khung bản đồ {@code bbox=minLng,minLat,maxLng,maxLat} của {@code GET /items} (06 §6.2). Không hỗ trợ khung vắt qua kinh tuyến 180. */
public record Bbox(double minLng, double minLat, double maxLng, double maxLat) {

    /** @return null nếu không gửi; sai định dạng hoặc ngoài phạm vi → 400 */
    public static Bbox parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split(",");
        try {
            if (parts.length == 4) {
                Bbox b = new Bbox(Double.parseDouble(parts[0].strip()), Double.parseDouble(parts[1].strip()),
                        Double.parseDouble(parts[2].strip()), Double.parseDouble(parts[3].strip()));
                if (b.valid()) {
                    return b;
                }
            }
        } catch (NumberFormatException e) {
            // rơi xuống lỗi chung bên dưới
        }
        throw ApiException.validation(List.of(new FieldError("bbox", "Khung bản đồ phải là minLng,minLat,maxLng,maxLat")));
    }

    private boolean valid() {
        return Double.isFinite(minLng) && Double.isFinite(minLat) && Double.isFinite(maxLng) && Double.isFinite(maxLat)
                && -180 <= minLng && minLng <= maxLng && maxLng <= 180
                && -90 <= minLat && minLat <= maxLat && maxLat <= 90;
    }
}
