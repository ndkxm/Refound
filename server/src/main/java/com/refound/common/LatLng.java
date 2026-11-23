package com.refound.common;

/** Vị trí trong JSON gửi lên {@code {"lat", "lng"}}; thiếu một trong hai thì {@link #toPoint()} là null, không âm thầm thành 0. */
public record LatLng(Double lat, Double lng) {

    public GeoPoint toPoint() {
        return lat == null || lng == null ? null : new GeoPoint(lat, lng);
    }
}
