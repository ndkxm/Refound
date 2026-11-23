package com.refound.auth.infrastructure;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Kiểm Firebase ID token không cần Admin SDK (ADR-05, U18): chữ ký RS256 theo JWKS của Google,
 * {@code iss = https://securetoken.google.com/<projectId>}, {@code aud = <projectId>}, còn hạn, {@code sub} khác rỗng.
 */
@Component
public class FirebaseTokenVerifier implements IdentityVerifier {

    private static final OAuth2Error INVALID = new OAuth2Error("invalid_token");

    private final NimbusJwtDecoder decoder;

    public FirebaseTokenVerifier(
            @Value("${refound.firebase.project-id}") String projectId,
            @Value("${refound.firebase.jwks-uri}") String jwksUri) {
        if (projectId.isBlank()) {
            throw new IllegalStateException("Thiếu FIREBASE_PROJECT_ID");
        }
        decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/" + projectId),
                jwt -> jwt.getAudience() != null && jwt.getAudience().contains(projectId) ? ok() : fail(),
                jwt -> jwt.getSubject() != null && !jwt.getSubject().isBlank() ? ok() : fail(),
                googleProviderOnly()));
    }

    @Override
    public VerifiedIdentity verify(String idToken) {
        try {
            Jwt jwt = decoder.decode(idToken);
            return new VerifiedIdentity(jwt.getSubject(), jwt.getClaimAsString("name"));
        } catch (JwtException e) {
            throw new InvalidIdentityTokenException("Firebase ID token không hợp lệ", e);
        }
    }

    /**
     * Chỉ bật đăng nhập Google (D10). Tài liệu Firebase không liệt {@code firebase.sign_in_provider} là claim bắt buộc
     * (ghi chú S05), nên thiếu claim thì cho qua; có mà khác {@code google.com} (vd. anonymous) thì từ chối (X23).
     */
    private static OAuth2TokenValidator<Jwt> googleProviderOnly() {
        return jwt -> {
            Object firebase = jwt.getClaims().get("firebase");
            if (!(firebase instanceof Map<?, ?> m) || m.get("sign_in_provider") == null) {
                return ok();
            }
            return "google.com".equals(m.get("sign_in_provider")) ? ok() : fail();
        };
    }

    private static OAuth2TokenValidatorResult ok() {
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult fail() {
        return OAuth2TokenValidatorResult.failure(INVALID);
    }
}
