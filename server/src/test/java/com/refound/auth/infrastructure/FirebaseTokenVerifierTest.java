package com.refound.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.refound.auth.infrastructure.IdentityVerifier.InvalidIdentityTokenException;
import com.refound.support.FakeFirebaseIdp;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** X23 ở mức hàm: chữ ký, {@code iss}, {@code aud}, hạn, nhà cung cấp đăng nhập (ADR-05, U18). */
class FirebaseTokenVerifierTest {

    static FakeFirebaseIdp idp;
    static FirebaseTokenVerifier verifier;

    @BeforeAll
    static void start() throws Exception {
        idp = FakeFirebaseIdp.start("refound-test");
        verifier = new FirebaseTokenVerifier("refound-test", idp.jwksUri());
    }

    @AfterAll
    static void stop() {
        idp.close();
    }

    @Test
    void validGoogleTokenGivesUidAndName() {
        Map<String, Object> claims = idp.validClaims("uid-1");
        claims.put("name", "Nguyễn Văn A");
        assertThat(verifier.verify(idp.token(claims)))
                .isEqualTo(new IdentityVerifier.VerifiedIdentity("uid-1", "Nguyễn Văn A"));
    }

    @Test
    void missingSignInProviderIsAccepted() {
        Map<String, Object> claims = idp.validClaims("uid-2");
        claims.remove("firebase"); // ghi chú S05: claim này không bắt buộc theo tài liệu
        assertThat(verifier.verify(idp.token(claims)).uid()).isEqualTo("uid-2");
    }

    @Test
    void signedByOtherKeyRejected() {
        assertRejected(idp.tokenSignedByOtherKey(idp.validClaims("uid")));
    }

    @Test
    void otherProjectAudienceRejected() {
        assertRejected(tokenWith(c -> c.put("aud", "project-khac")));
    }

    @Test
    void otherIssuerRejected() {
        assertRejected(tokenWith(c -> c.put("iss", "https://securetoken.google.com/project-khac")));
    }

    @Test
    void expiredRejected() {
        long now = Instant.now().getEpochSecond();
        assertRejected(tokenWith(c -> {
            c.put("iat", now - 7200);
            c.put("exp", now - 3600);
        }));
    }

    @Test
    void anonymousProviderRejected() {
        assertRejected(tokenWith(c -> c.put("firebase", Map.of("sign_in_provider", "anonymous"))));
    }

    @Test
    void blankSubjectRejected() {
        assertRejected(tokenWith(c -> c.put("sub", "")));
    }

    @Test
    void garbageRejected() {
        assertRejected("khong-phai-jwt");
    }

    private static String tokenWith(Consumer<Map<String, Object>> change) {
        Map<String, Object> claims = idp.validClaims("uid");
        change.accept(claims);
        return idp.token(claims);
    }

    private static void assertRejected(String token) {
        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(InvalidIdentityTokenException.class);
    }
}
