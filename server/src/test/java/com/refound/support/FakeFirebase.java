package com.refound.support;

import com.refound.auth.infrastructure.FirebaseTokenVerifier;
import com.refound.auth.infrastructure.IdentityVerifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Dùng {@link FirebaseTokenVerifier} thật, trỏ vào JWKS của {@link FakeFirebaseIdp} (09 §9.2, X23):
 * test đăng nhập đi qua đúng đường kiểm chữ ký, {@code iss}, {@code aud}, hạn như production.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeFirebase {

    public static final String PROJECT_ID = "refound-test";
    public static final FakeFirebaseIdp IDP = start();

    @Bean
    @Primary
    IdentityVerifier fakeFirebaseVerifier() {
        return new FirebaseTokenVerifier(PROJECT_ID, IDP.jwksUri());
    }

    // Sống suốt lượt chạy test (HttpServer của JDK, cổng ngẫu nhiên); JVM tắt thì đóng theo.
    private static FakeFirebaseIdp start() {
        try {
            return FakeFirebaseIdp.start(PROJECT_ID);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
