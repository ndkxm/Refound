package com.refound.auth.application;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/** JWT riêng của server (ADR-05): HS256, {@code sub=userId}, {@code role}, hạn mặc định 7 ngày. Không có refresh token. */
@Component
public class JwtIssuer {

    static final String ISSUER = "refound";

    private final SecretKey key;
    private final Duration ttl;
    private final Clock clock;
    private final NimbusJwtEncoder encoder;

    @Autowired
    public JwtIssuer(
            @Value("${refound.jwt.secret}") String secret,
            @Value("${refound.jwt.expiration}") long expirationMs,
            Clock clock) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            // HS256 cần khóa ≥ 256 bit; dừng ngay lúc khởi động thay vì phát token yếu.
            throw new IllegalStateException("JWT_SECRET phải dài ít nhất 32 byte");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
        this.ttl = Duration.ofMillis(expirationMs);
        this.clock = clock;
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    public String issue(UUID userId, String role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    /** Decoder cho mọi request: kiểm chữ ký HS256 và hạn (mặc định của Spring, lệch giờ 60 s). */
    public JwtDecoder decoder() {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
