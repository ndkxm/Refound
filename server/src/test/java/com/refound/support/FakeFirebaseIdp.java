package com.refound.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase giả cho test X23 (docs/kien-truc/09-kiem-thu.md 9.2): sinh khóa RSA, phục vụ JWKS qua HTTP cục bộ
 * và ký ID token RS256 giống Firebase. FirebaseTokenVerifier thật (P0-02) trỏ {@link #jwksUri()} vào đây.
 * Dùng: {@code try (var idp = FakeFirebaseIdp.start("refound-test")) { ... }}.
 */
public final class FakeFirebaseIdp implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    static final String KID = "test-key";

    private final String projectId;
    private final KeyPair key = rsa();
    private final KeyPair otherKey = rsa(); // khóa không có trong JWKS, cho ca "ký bằng khóa khác"
    private final HttpServer server;

    private FakeFirebaseIdp(String projectId) throws Exception {
        this.projectId = projectId;
        byte[] body = JSON.writeValueAsBytes(Map.of("keys", List.of(jwk((RSAPublicKey) key.getPublic()))));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", ex -> {
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (var out = ex.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    public static FakeFirebaseIdp start(String projectId) throws Exception {
        return new FakeFirebaseIdp(projectId);
    }

    public String jwksUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks";
    }

    public String issuer() {
        return "https://securetoken.google.com/" + projectId;
    }

    public RSAPublicKey publicKey() {
        return (RSAPublicKey) key.getPublic();
    }

    /** Claim hợp lệ như token Firebase thật (U18), còn hạn 1 giờ, đăng nhập bằng Google. */
    public Map<String, Object> validClaims(String uid) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> c = new HashMap<>();
        c.put("iss", issuer());
        c.put("aud", projectId);
        c.put("sub", uid);
        c.put("user_id", uid);
        c.put("iat", now);
        c.put("auth_time", now);
        c.put("exp", now + 3600);
        c.put("firebase", Map.of("sign_in_provider", "google.com"));
        return c;
    }

    public String token(Map<String, Object> claims) {
        return sign(claims, key.getPrivate());
    }

    public String tokenSignedByOtherKey(Map<String, Object> claims) {
        return sign(claims, otherKey.getPrivate());
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static String sign(Map<String, Object> claims, PrivateKey privateKey) {
        try {
            String input = B64.encodeToString(JSON.writeValueAsBytes(Map.of("alg", "RS256", "kid", KID, "typ", "JWT")))
                    + "." + B64.encodeToString(JSON.writeValueAsBytes(claims));
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(privateKey);
            s.update(input.getBytes(StandardCharsets.US_ASCII));
            return input + "." + B64.encodeToString(s.sign());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> jwk(RSAPublicKey pub) {
        return Map.of(
                "kty", "RSA", "alg", "RS256", "use", "sig", "kid", KID,
                "n", B64.encodeToString(unsigned(pub.getModulus().toByteArray())),
                "e", B64.encodeToString(unsigned(pub.getPublicExponent().toByteArray())));
    }

    // BigInteger.toByteArray() có thể thêm byte 0 dẫn đầu; JWK yêu cầu số không dấu.
    private static byte[] unsigned(byte[] b) {
        return b.length > 1 && b[0] == 0 ? java.util.Arrays.copyOfRange(b, 1, b.length) : b;
    }

    private static KeyPair rsa() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
