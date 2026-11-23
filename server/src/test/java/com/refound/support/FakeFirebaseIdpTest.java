package com.refound.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Kiểm chính fixture: JWKS phục vụ qua HTTP xác minh được token ký bằng khóa chính, không xác minh được token khóa khác. */
class FakeFirebaseIdpTest {

    @Test
    void jwksVerifiesOwnTokenButNotOtherKey() throws Exception {
        try (var idp = FakeFirebaseIdp.start("refound-test")) {
            String body = HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder(URI.create(idp.jwksUri())).build(), HttpResponse.BodyHandlers.ofString())
                    .body();
            JsonNode k = new ObjectMapper().readTree(body).get("keys").get(0);
            assertThat(k.get("kid").asText()).isEqualTo(FakeFirebaseIdp.KID);

            Base64.Decoder d = Base64.getUrlDecoder();
            PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                    new BigInteger(1, d.decode(k.get("n").asText())), new BigInteger(1, d.decode(k.get("e").asText()))));
            assertThat(pub).isEqualTo(idp.publicKey());

            var claims = idp.validClaims("uid-a");
            assertThat(verifies(idp.token(claims), pub)).isTrue();
            assertThat(verifies(idp.tokenSignedByOtherKey(claims), pub)).isFalse();

            String payload = new String(d.decode(idp.token(claims).split("\\.")[1]), StandardCharsets.UTF_8);
            assertThat(payload).contains("\"aud\":\"refound-test\"", "\"sign_in_provider\":\"google.com\"");
        }
    }

    private static boolean verifies(String jwt, PublicKey pub) throws Exception {
        String[] p = jwt.split("\\.");
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(pub);
        s.update((p[0] + "." + p[1]).getBytes(StandardCharsets.US_ASCII));
        return s.verify(Base64.getUrlDecoder().decode(p[2]));
    }
}
