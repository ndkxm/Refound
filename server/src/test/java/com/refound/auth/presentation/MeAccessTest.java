package com.refound.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.auth.application.JwtIssuer;
import com.refound.support.FakeStorage;
import com.refound.support.IntegrationTest;
import com.refound.support.TestUsers;
import com.refound.support.TestUsers.TestUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Hồ sơ (FR-AUTH-02) và các test truy cập chéo X17, X19, token hết hạn / sai, /admin của user thường. */
@IntegrationTest
class MeAccessTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    TestUsers users;

    @Autowired
    FakeStorage storage;

    @Value("${refound.jwt.secret}")
    String jwtSecret;

    @Test
    void meReturnsOwnProfileWithoutFirebaseUid() throws Exception {
        TestUser a = users.a();
        mvc.perform(get("/api/v1/me").header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.id().toString()))
                .andExpect(jsonPath("$.displayName").value("Người A"))
                .andExpect(jsonPath("$.reputation").value(0))
                .andExpect(jsonPath("$.authProvider").value("google"))
                .andExpect(jsonPath("$.notificationPrefs.match").value(true))
                .andExpect(jsonPath("$.firebaseUid").doesNotExist());
    }

    @Test
    void noTokenIs401() throws Exception {
        unauthenticated(mvc.perform(get("/api/v1/me")));
    }

    @Test
    void garbageTokenIs401() throws Exception {
        unauthenticated(mvc.perform(get("/api/v1/me").header("Authorization", "Bearer khong-phai-jwt")));
    }

    @Test
    void expiredTokenIs401() throws Exception {
        TestUser a = users.a();
        Clock eightDaysAgo = Clock.fixed(Instant.now().minus(8, ChronoUnit.DAYS), ZoneOffset.UTC);
        String expired = new JwtIssuer(jwtSecret, 604_800_000L, eightDaysAgo).issue(a.id(), "user");
        unauthenticated(mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + expired)));
    }

    @Test
    void tokenSignedWithOtherSecretIs401() throws Exception {
        TestUser a = users.a();
        String forged = new JwtIssuer("x".repeat(32), 604_800_000L, Clock.systemUTC()).issue(a.id(), "admin");
        unauthenticated(mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + forged)));
    }

    /** X19: tài khoản bị khóa / đã xóa, JWT còn hạn → 401 ở mọi endpoint. */
    @ParameterizedTest(name = "X19 {0} → 401")
    @ValueSource(strings = {"locked", "deleted"})
    void inactiveAccountWithValidJwtIs401(String status) throws Exception {
        TestUser a = users.a();
        TestUser b = users.b();
        users.setStatus(a, status);

        unauthenticated(mvc.perform(get("/api/v1/me").header("Authorization", a.bearer())));
        unauthenticated(mvc.perform(get("/api/v1/users/" + b.id()).header("Authorization", a.bearer())));
        unauthenticated(mvc.perform(patch("/api/v1/me").header("Authorization", a.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Tên mới\"}")));
    }

    /** X17: B xem A chỉ thấy tên, ảnh, uy tín. */
    @Test
    void otherUserSeesOnlyPublicFields() throws Exception {
        TestUser a = users.a();
        TestUser b = users.b();

        String body = mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", b.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Người A"))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> fields = json.readValue(body, new TypeReference<>() {});
        assertThat(fields).containsOnlyKeys("id", "displayName", "avatarUrl", "reputation");
    }

    @Test
    void unknownUserIs404() throws Exception {
        mvc.perform(get("/api/v1/users/" + UUID.randomUUID()).header("Authorization", users.a().bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void renameStripsAndSaves() throws Exception {
        TestUser a = users.a();
        mvc.perform(patch("/api/v1/me").header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"  Kiên  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Kiên"));
        mvc.perform(get("/api/v1/me").header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.displayName").value("Kiên"));
    }

    @Test
    void renameTooShortIs400() throws Exception {
        mvc.perform(patch("/api/v1/me").header("Authorization", users.a().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\" K \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("displayName"));
    }

    @Test
    void avatarUploadStoresJpegAndOthersSeeSignedUrl() throws Exception {
        TestUser a = users.a();
        String path = "avatars/" + a.id() + ".jpg";

        uploadAvatar(a, "image/jpeg", JPEG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://storage.test/" + path));

        assertThat(storage.file(path)).isEqualTo(JPEG);
        mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", users.b().bearer()))
                .andExpect(jsonPath("$.avatarUrl").value("https://storage.test/" + path));
    }

    @Test
    void avatarNotJpegIs400EvenIfContentTypeSaysSo() throws Exception {
        uploadAvatar(users.a(), "image/jpeg", "\u0089PNG....".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void avatarOver1MbIs413() throws Exception {
        byte[] big = Arrays.copyOf(JPEG, 1024 * 1024 + 1);
        uploadAvatar(users.a(), "image/jpeg", big)
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));
    }

    /** /admin/** đã khóa theo URL từ giờ (X18 đầy đủ thuộc P1-10). */
    @Test
    void adminPathForbiddenForUserButPassesForAdmin() throws Exception {
        mvc.perform(get("/api/v1/admin/reports").header("Authorization", users.a().bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/v1/admin/reports").header("Authorization", users.admin().bearer()))
                .andExpect(status().isNotFound()); // qua được lớp quyền, chưa có endpoint
    }

    /** Quyền lấy từ DB mỗi request: hạ admin xuống user có tác dụng ngay dù JWT ghi role=admin. */
    @Test
    void demotedAdminLosesAccessImmediately() throws Exception {
        TestUser admin = users.admin();
        users.setRole(admin, "user");
        mvc.perform(get("/api/v1/admin/reports").header("Authorization", admin.bearer()))
                .andExpect(status().isForbidden());
    }

    private ResultActions uploadAvatar(TestUser user, String contentType, byte[] bytes) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, "/api/v1/me/avatar")
                .file(new MockMultipartFile("file", "avatar.jpg", contentType, bytes))
                .header("Authorization", user.bearer()));
    }

    private static void unauthenticated(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
