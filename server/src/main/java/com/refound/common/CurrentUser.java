package com.refound.common;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** userId chỉ lấy từ JWT của server, không bao giờ từ body (03 §3.3). */
public final class CurrentUser {

    private CurrentUser() {}

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    /** Quyền admin lấy từ authority do UserStatusFilter gán theo DB, nên hạ quyền có tác dụng ngay. */
    public static Viewer viewer(JwtAuthenticationToken auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return new Viewer(id(auth.getToken()), admin);
    }
}
