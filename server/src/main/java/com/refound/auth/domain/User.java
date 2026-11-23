package com.refound.auth.domain;

import java.util.UUID;

/** Một dòng {@code users}. Không có email hay mật khẩu (ADR-05, BR-04). */
public record User(
        UUID id,
        String firebaseUid,
        String displayName,
        String avatarPath,
        String role,
        String status,
        int reputation,
        NotificationPrefs notificationPrefs) {

    public boolean isActive() {
        return "active".equals(status);
    }
}
