package com.refound.auth.presentation;

import static com.refound.support.FakeFirebase.IDP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.auth.application.JwtIssuer;
import com.refound.support.IntegrationTest;
import com.refound.support.TestUsers;
import com.refound.support.TestUsers.TestUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** {@code POST /auth/firebase} (FR-AUTH-01, ADR-05) và X23, đi qua FirebaseTokenVerifier thật với JWKS giả. */
@IntegrationTest
class AuthAccessTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestUsers users;

    @Value("${refound.jwt.secret}")
    String jwtSecret;

    @Test
    void firstLoginCreatesUserWithGoogleNameAndTokenWorks() throws Exception {
        String uid = "uid-" + UUID.randomUUID();
        Map<String, Object> claims = IDP.validClaims(uid);
        claims.put("name", "Nguyễn Văn A");

        JsonNode body = read(login(IDP.token(claims), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNew").value(true))
                .andExpect(jsonPath("$.user.displayName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.user.authProvider").value("google"))
                .andExpect(jsonPath("$.user.role").value("user")));

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + body.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(body.at("/user/id").asText()));
        assertThat(jdbc.queryForObject("select count(*) from refound.users where firebase_uid = ?", Integer.class, uid))
                .isEqualTo(1);
    }

    @Test
    void requestedDisplayNameIsStrippedAndUsed() throws Exception {
        login(IDP.token(IDP.validClaims("uid-" + UUID.randomUUID())), "  Kiên  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.displayName").value("Kiên"));
    }

    @Test
    void secondLoginReturnsSameUserNotNew() throws Exception {
        String token = IDP.token(IDP.validClaims("uid-" + UUID.randomUUID()));
        String firstId = read(login(token, null)).at("/user/id").asText();

        login(token, "Tên khác")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNew").value(false))
                .andExpect(jsonPath("$.user.id").value(firstId))
                .andExpect(jsonPath("$.user.displayName").value("Người dùng")); // tên cũ, không bị ghi đè
    }

    static Stream<Arguments> forgedTokens() {
        long now = Instant.now().getEpochSecond();
        return Stream.of(
                Arguments.of("ký bằng khóa khác", IDP.tokenSignedByOtherKey(IDP.validClaims("x23-a"))),
                Arguments.of("sai aud", IDP.token(with(IDP.validClaims("x23-b"), "aud", "project-khac"))),
                Arguments.of("hết hạn", IDP.token(with(with(IDP.validClaims("x23-c"), "iat", now - 7200), "exp", now - 3600))),
                Arguments.of("anonymous", IDP.token(with(IDP.validClaims("x23-d"), "firebase", Map.of("sign_in_provider", "anonymous")))));
    }

    /** X23: token giả không đăng nhập được và không tạo user. */
    @ParameterizedTest(name = "X23 {0} → 401")
    @MethodSource("forgedTokens")
    void forgedFirebaseTokenRejected(String why, String token) throws Exception {
        login(token, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        assertThat(jdbc.queryForObject("select count(*) from refound.users where firebase_uid like 'x23-%'", Integer.class))
                .isZero();
    }

    @Test
    void lockedAccountCannotLogIn() throws Exception {
        TestUser a = users.a();
        users.setStatus(a, "locked");

        login(IDP.token(IDP.validClaims(a.firebaseUid())), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void missingIdTokenIsValidationError() throws Exception {
        mvc.perform(post("/api/v1/auth/firebase").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("idToken"));
    }

    @Test
    void tooShortDisplayNameIsValidationError() throws Exception {
        login(IDP.token(IDP.validClaims("uid-" + UUID.randomUUID())), " K ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("displayName"));
    }

    @Test
    void expiredServerJwtInHeaderDoesNotBlockLogin() throws Exception {
        Clock eightDaysAgo = Clock.fixed(Instant.now().minus(8, ChronoUnit.DAYS), ZoneOffset.UTC);
        String expired = new JwtIssuer(jwtSecret, 604_800_000L, eightDaysAgo).issue(UUID.randomUUID(), "user");

        mvc.perform(post("/api/v1/auth/firebase")
                        .header("Authorization", "Bearer " + expired)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idToken", IDP.token(IDP.validClaims("uid-" + UUID.randomUUID()))))))
                .andExpect(status().isOk());
    }

    private ResultActions login(String idToken, String displayName) throws Exception {
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("idToken", idToken);
        if (displayName != null) {
            req.put("displayName", displayName);
        }
        return mvc.perform(post("/api/v1/auth/firebase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(req)));
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static Map<String, Object> with(Map<String, Object> claims, String key, Object value) {
        claims.put(key, value);
        return claims;
    }
}
