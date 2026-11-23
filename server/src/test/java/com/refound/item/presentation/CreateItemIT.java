package com.refound.item.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.support.FakeStorage;
import com.refound.support.IntegrationTest;
import com.refound.support.TestUsers;
import com.refound.support.TestUsers.TestUser;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** P0-05: {@code POST /items} (I1, BR-09, BR-11, BR-13, ADR-06, 7.3) và {@code GET /categories}. */
@IntegrationTest
class CreateItemIT {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    TestUsers users;

    @Autowired
    FakeStorage storage;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void foundItemWithImagesIsStoredWithRoundedPublicLocation() throws Exception {
        TestUser a = users.a();
        String body = create(a, found(), 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("found"))
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.authorId").value(a.id().toString()))
                .andExpect(jsonPath("$.description").value("Ví da nâu, có thẻ xe"))
                .andExpect(jsonPath("$.exactLocation.lat").value(10.8705))
                .andExpect(jsonPath("$.exactLocation.lng").value(106.8035))
                .andExpect(jsonPath("$.publicLocation.lat").value(10.871))
                .andExpect(jsonPath("$.publicLocation.lng").value(106.804))
                .andExpect(jsonPath("$.radiusM").doesNotExist())
                .andExpect(jsonPath("$.embeddingStatus").value("pending"))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[1].position").value(1))
                .andExpect(jsonPath("$.answer").doesNotExist())
                .andExpect(jsonPath("$.question").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(json.readTree(body).get("id").asText());
        assertThat(json.readTree(body).get("happenedAt").asText()).endsWith("+07:00"); // BR-12
        assertThat(json.readTree(body).at("/images/0/thumbUrl").asText())
                .isEqualTo("https://storage.test/items/" + id + "/0_thumb.jpg");
        assertThat(storage.file("items/" + id + "/1.jpg")).isEqualTo(JPEG);

        // kinh độ/vĩ độ không bị đảo khi ghi geography (st_makepoint(lng, lat))
        Map<String, Object> row = jdbc.queryForMap(
                "select st_y(location::geometry) lat, st_x(location::geometry) lng, st_y(public_location::geometry) plat, "
                        + "embedding_status, embedding_attempts from refound.items where id = ?", id);
        assertThat(row).containsEntry("lat", 10.8705).containsEntry("lng", 106.8035).containsEntry("plat", 10.871)
                .containsEntry("embedding_status", "pending").containsEntry("embedding_attempts", 0);
        assertThat(jdbc.queryForMap("select question, answer from refound.verification_secrets where item_id = ?", id))
                .containsEntry("question", "Trong ví có gì?").containsEntry("answer", "Thẻ xe");
    }

    @Test
    void lostItemWithoutImagesKeepsExactAreaPublic() throws Exception {
        create(users.a(), lost(), 0)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("lost"))
                .andExpect(jsonPath("$.publicLocation.lat").value(10.870345))
                .andExpect(jsonPath("$.radiusM").value(200))
                .andExpect(jsonPath("$.images.length()").value(0));
    }

    /** RN FormData gửi phần chuỗi không có Content-Type; Spring phải đọc được, kể cả tiếng Việt. */
    @Test
    void dataPartWithoutContentTypeIsAccepted() throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/v1/items");
        req.part(new MockPart("data", json.writeValueAsBytes(lost())));
        mvc.perform(req.header("Authorization", users.a().bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Tai nghe trắng, hộp sứt góc"));
    }

    @Test
    void invalidDataListsEveryField() throws Exception {
        Map<String, Object> bad = new LinkedHashMap<>();
        bad.put("type", "found");
        bad.put("category", "khong-co");
        bad.put("description", "abc");
        bad.put("happenedAt", OffsetDateTime.now(ZoneOffset.ofHours(7)).plusDays(1).toString());
        bad.put("location", Map.of("lat", 10.87)); // thiếu lng: không được coi là 0
        bad.put("radiusM", 100);
        create(users.a(), bad, 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "category", "description", "happenedAt", "location", "radiusM", "full", "question", "answer")));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/v1/items");
        req.part(new MockPart("data", "{khong phai json".getBytes(StandardCharsets.UTF_8)));
        mvc.perform(req.header("Authorization", users.a().bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(multipart("/api/v1/items").header("Authorization", users.a().bearer()))
                .andExpect(status().isBadRequest()); // thiếu phần data
    }

    @Test
    void lostSensitiveItemCannotHaveImages() throws Exception {
        Map<String, Object> card = lost();
        card.put("category", "student_card");
        create(users.a(), card, 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("full"));
    }

    @Test
    void imagesMustBeRealJpegPairs() throws Exception {
        TestUser a = users.a();
        MockMultipartHttpServletRequestBuilder png = request(a, found());
        png.file(new MockMultipartFile("full", "a.jpg", "image/jpeg", "\u0089PNG....".getBytes(StandardCharsets.ISO_8859_1)));
        png.file(new MockMultipartFile("thumb", "a.jpg", "image/jpeg", JPEG));
        mvc.perform(png).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("full[0]"));

        MockMultipartHttpServletRequestBuilder unpaired = request(a, found());
        unpaired.file(new MockMultipartFile("full", "a.jpg", "image/jpeg", JPEG));
        mvc.perform(unpaired).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("thumb"));

        create(a, found(), 5).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("full"));
    }

    @Test
    void imageOver1MbIs413() throws Exception {
        MockMultipartHttpServletRequestBuilder req = request(users.a(), found());
        req.file(new MockMultipartFile("full", "a.jpg", "image/jpeg", Arrays.copyOf(JPEG, 1024 * 1024 + 1)));
        req.file(new MockMultipartFile("thumb", "a.jpg", "image/jpeg", JPEG));
        mvc.perform(req).andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));
    }

    /** BR-09: tin thứ 11 trong 24 giờ → 429 kèm thời gian chờ; tin cũ hơn 24 giờ và tin đã xóa vẫn tính đúng. */
    @Test
    void eleventhItemIn24HoursIsRateLimited() throws Exception {
        TestUser a = users.a();
        Instant now = Instant.now();
        insertItemCreatedAt(a.id(), now.minus(25, ChronoUnit.HOURS)); // ngoài cửa sổ
        for (int h = 1; h <= 9; h++) {
            insertItemCreatedAt(a.id(), now.minus(h, ChronoUnit.HOURS));
        }
        jdbc.update("update refound.items set deleted_at = now() where author_id = ? and created_at > now() - interval '2 hours'",
                a.id()); // xóa rồi vẫn đếm
        create(a, lost(), 0).andExpect(status().isCreated()); // tin thứ 10

        String body = create(a, lost(), 0)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andReturn().getResponse().getContentAsString();
        long retry = json.readTree(body).get("retryAfterSeconds").asLong();
        // tin cũ nhất trong cửa sổ tạo 9 giờ trước → còn khoảng 15 giờ
        assertThat(retry).isBetween(15 * 3600L - 120, 15 * 3600L);
        assertThat(count(a.id())).isEqualTo(11);
        assertThat(create(users.b(), lost(), 0).andReturn().getResponse().getStatus()).isEqualTo(201); // người khác không bị ảnh hưởng
    }

    /** 7.3: Storage lỗi → 503, DB không có tin nào. */
    @Test
    void storageFailureIs503AndWritesNothing() throws Exception {
        TestUser a = users.a();
        storage.failUploads = true;
        try {
            create(a, found(), 1)
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
        } finally {
            storage.failUploads = false;
        }
        assertThat(count(a.id())).isZero();
    }

    @Test
    void unauthenticatedIs401() throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/v1/items");
        req.part(new MockPart("data", json.writeValueAsBytes(lost())));
        mvc.perform(req).andExpect(status().isUnauthorized());
    }

    @Test
    void categoriesListsFourteenWithSensitiveFlag() throws Exception {
        mvc.perform(get("/api/v1/categories").header("Authorization", users.a().bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(14))
                .andExpect(jsonPath("$[?(@.code == 'student_card')].sensitive").value(true))
                .andExpect(jsonPath("$[?(@.code == 'wallet')].nameVi").value("Ví"));
    }

    // --- hỗ trợ ---

    static Map<String, Object> found() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "found");
        m.put("category", "wallet");
        m.put("description", "  Ví da nâu, có thẻ xe  ");
        m.put("happenedAt", "2026-11-20T09:00:00+07:00");
        m.put("location", Map.of("lat", 10.8705, "lng", 106.8035));
        m.put("question", "Trong ví có gì?");
        m.put("answer", "Thẻ xe");
        return m;
    }

    static Map<String, Object> lost() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "lost");
        m.put("category", "earphones");
        m.put("description", "Tai nghe trắng, hộp sứt góc");
        m.put("happenedAt", "2026-11-20T09:00:00+07:00");
        m.put("location", Map.of("lat", 10.870345, "lng", 106.803128));
        m.put("radiusM", 200);
        return m;
    }

    private ResultActions create(TestUser user, Map<String, Object> data, int images) throws Exception {
        MockMultipartHttpServletRequestBuilder req = request(user, data);
        for (int i = 0; i < images; i++) {
            req.file(new MockMultipartFile("full", "f" + i + ".jpg", "image/jpeg", JPEG));
            req.file(new MockMultipartFile("thumb", "t" + i + ".jpg", "image/jpeg", JPEG));
        }
        return mvc.perform(req);
    }

    private MockMultipartHttpServletRequestBuilder request(TestUser user, Map<String, Object> data) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/v1/items");
        req.file(new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, json.writeValueAsBytes(data)));
        req.header("Authorization", user.bearer());
        return req;
    }

    private void insertItemCreatedAt(UUID author, Instant createdAt) {
        jdbc.update("""
                insert into refound.items (author_id, type, category, description, happened_at, location, public_location,
                  radius_m, created_at)
                values (?, 'lost', 'wallet', 'Ví cũ đã đăng', now(), st_makepoint(106.8, 10.87)::geography,
                  st_makepoint(106.8, 10.87)::geography, 100, ?)
                """, author, OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
    }

    private int count(UUID author) {
        return jdbc.queryForObject("select count(*) from refound.items where author_id = ?", Integer.class, author);
    }
}
