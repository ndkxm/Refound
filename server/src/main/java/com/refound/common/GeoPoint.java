package com.refound.common;

/** Tọa độ WGS84 dạng {@code {"lat": 10.87, "lng": 106.80}} (06 §6.1). Lưu DB là {@code geography(Point, 4326)}. */
public record GeoPoint(double lat, double lng) {

    public boolean isValid() {
        return Double.isFinite(lat) && Double.isFinite(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }
}
