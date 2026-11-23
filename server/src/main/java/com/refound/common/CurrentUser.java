package com.refound.common;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** userId chỉ lấy từ JWT của server, không bao giờ từ body (03 §3.3). */
public final class CurrentUser {

    private CurrentUser() {}

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
