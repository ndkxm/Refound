package com.refound.auth.application;

import com.refound.auth.domain.User;
import com.refound.auth.infrastructure.IdentityVerifier;
import com.refound.auth.infrastructure.IdentityVerifier.InvalidIdentityTokenException;
import com.refound.auth.infrastructure.IdentityVerifier.VerifiedIdentity;
import com.refound.auth.infrastructure.UserRepository;
import com.refound.common.ApiException;
import org.springframework.stereotype.Service;

/** {@code POST /auth/firebase}: Firebase ID token → user (tạo nếu chưa có) → JWT của server (ADR-05). */
@Service
public class AuthService {

    static final String DEFAULT_NAME = "Người dùng";

    private final IdentityVerifier verifier;
    private final UserRepository users;
    private final JwtIssuer jwt;
    private final ProfileService profiles;

    public AuthService(IdentityVerifier verifier, UserRepository users, JwtIssuer jwt, ProfileService profiles) {
        this.verifier = verifier;
        this.users = users;
        this.jwt = jwt;
        this.profiles = profiles;
    }

    public LoginResult login(String idToken, String requestedName) {
        VerifiedIdentity id;
        try {
            id = verifier.verify(idToken);
        } catch (InvalidIdentityTokenException e) {
            throw ApiException.unauthenticated("Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn, vui lòng đăng nhập lại.");
        }

        boolean isNew = false;
        User user = users.findByFirebaseUid(id.uid()).orElse(null);
        if (user == null) {
            user = users.insertIfAbsent(id.uid(), initialName(requestedName, id.name())).orElse(null);
            isNew = user != null;
            if (user == null) { // lần đăng nhập song song khác vừa tạo xong
                user = users.findByFirebaseUid(id.uid()).orElseThrow();
            }
        }
        if (!user.isActive()) {
            throw ApiException.unauthenticated("Tài khoản đã bị khóa.");
        }
        return new LoginResult(jwt.issue(user.id(), user.role()), profiles.toMe(user), isNew);
    }

    /**
     * Tên lúc tạo tài khoản: tên app gửi (đã kiểm 2–40 ký tự), nếu không có thì tên Google cắt còn 40 ký tự,
     * nếu vẫn ngắn hơn 2 ký tự thì "Người dùng". App vẫn mở màn đặt tên khi {@code isNew} (06 §6.2).
     */
    static String initialName(String requested, String googleName) {
        if (requested != null) {
            return requested;
        }
        String name = googleName == null ? "" : googleName.strip();
        if (name.codePointCount(0, name.length()) > 40) {
            name = name.substring(0, name.offsetByCodePoints(0, 40)).strip();
        }
        return name.codePointCount(0, name.length()) >= 2 ? name : DEFAULT_NAME;
    }

    public record LoginResult(String token, MeDto user, boolean isNew) {}
}
