package com.refound.auth.domain;

import java.util.Map;

/** Bật/tắt từng loại thông báo (FR-NOTI-03). Cột jsonb mặc định {@code {}}: loại nào chưa đặt thì coi là bật. */
public record NotificationPrefs(boolean match, boolean claim, boolean chat, boolean expiry) {

    public static NotificationPrefs from(Map<String, ?> stored) {
        return new NotificationPrefs(on(stored, "match"), on(stored, "claim"), on(stored, "chat"), on(stored, "expiry"));
    }

    private static boolean on(Map<String, ?> stored, String key) {
        return !Boolean.FALSE.equals(stored.get(key));
    }
}
