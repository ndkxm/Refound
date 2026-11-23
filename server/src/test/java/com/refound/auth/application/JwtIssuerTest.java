package com.refound.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtIssuerTest {

    static final String SECRET = "0123456789abcdef0123456789abcdef"; // đúng 32 byte
    static final long SEVEN_DAYS_MS = 604_800_000L;

    @Test
    void issuedTokenDecodesWithSubjectRoleAndSevenDayExpiry() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtIssuer issuer = new JwtIssuer(SECRET, SEVEN_DAYS_MS, Clock.fixed(now, ZoneOffset.UTC));
        UUID id = UUID.randomUUID();

        Jwt jwt = issuer.decoder().decode(issuer.issue(id, "admin"));

        assertThat(jwt.getSubject()).isEqualTo(id.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("admin");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(7, ChronoUnit.DAYS));
    }

    @Test
    void expiredTokenRejected() {
        Clock eightDaysAgo = Clock.fixed(Instant.now().minus(8, ChronoUnit.DAYS), ZoneOffset.UTC);
        JwtIssuer old = new JwtIssuer(SECRET, SEVEN_DAYS_MS, eightDaysAgo);
        String token = old.issue(UUID.randomUUID(), "user");

        assertThatThrownBy(() -> old.decoder().decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void tokenSignedWithOtherSecretRejected() {
        JwtIssuer other = new JwtIssuer("ffffffffffffffffffffffffffffffff", SEVEN_DAYS_MS, Clock.systemUTC());
        JwtIssuer ours = new JwtIssuer(SECRET, SEVEN_DAYS_MS, Clock.systemUTC());

        assertThatThrownBy(() -> ours.decoder().decode(other.issue(UUID.randomUUID(), "user")))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    @Test
    void shortSecretRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtIssuer("ngan-qua", SEVEN_DAYS_MS, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
