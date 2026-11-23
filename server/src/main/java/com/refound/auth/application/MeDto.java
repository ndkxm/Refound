package com.refound.auth.application;

import com.refound.auth.domain.NotificationPrefs;
import java.util.UUID;

/** Hồ sơ của chính mình. Không có {@code firebaseUid}: app không cần, càng ít lộ càng tốt. */
public record MeDto(
        UUID id,
        String displayName,
        String avatarUrl,
        String role,
        int reputation,
        String authProvider,
        NotificationPrefs notificationPrefs) {}
