package com.refound.matching.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.matching.application.AiCircuit;
import com.refound.matching.domain.EmbeddingText;
import com.refound.matching.infrastructure.EmbeddingClient.EmbeddingException;
import com.refound.support.FakeEmbeddingClient;
import com.refound.support.IntegrationTest;
import com.refound.support.TestItems;
import com.refound.support.TestUsers;
import com.refound.support.TestUsers.TestUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * P0-12 phần server: {@code POST /search} (04 §4.3). Vector câu truy vấn và vector tin do test đặt, nên cosine biết
 * trước. Mỗi test tìm quanh một tọa độ ngẫu nhiên riêng, lọc cứng 3 km giữ test khác ra ngoài.
 */
@IntegrationTest
class SearchIT {

    static final String QUERY = "tai nghe trắng";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    TestUsers users;

    @Autowired
    TestItems items;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FakeEmbeddingClient gemini;

    @Autowired
    AiCircuit circuit;

    double[] spot;
    TestUser a;
    TestUser b;

    @BeforeEach
    void setUp() {
        jdbc.update("delete from refound.embedding_cache"); // test khác có thể đã cache cùng câu truy vấn
        gemini.reset();
        circuit.reset();
        spot = TestItems.randomSpot();
        a = users.a();
        b = users.b();
        // câu truy vấn nằm trên trục 0; tin có vector withCosine(c, k) thì cosine với truy vấn đúng bằng c
        gemini.define(EmbeddingText.forQuery(QUERY), FakeEmbeddingClient.axis(0));
    }

    @AfterEach
    void tearDown() {
        gemini.reset();
        circuit.reset();
    }

    /** Xếp theo S; tin chưa có vector vẫn có mặt nhưng xếp sau và có lý do "đang xử lý". */
    @Test
    void ranksByScoreWithReasons() throws Exception {
        UUID strong = embedded(items.found(a).category("earphones").description("Tai nghe trắng").at(spot[0], spot[1]).insert(), 0.6, 1);
        UUID weak = embedded(items.found(a).category("earphones").description("Tai nghe đen").at(spot[0], spot[1]).insert(), 0.2, 2);
        UUID pending = items.found(a).category("wallet").at(spot[0], spot[1]).insert();

        JsonNode res = search(b, query("found")).andExpect(status().isOk()).andReturn().json();
        assertThat(res.get("degraded").asBoolean()).isFalse();
        assertThat(ids(res)).containsExactly(strong, weak, pending);
        JsonNode first = res.at("/results/0");
        assertThat(first.at("/components/v").asDouble()).isEqualTo(1.0);
        assertThat(first.at("/components/c").asDouble()).isEqualTo(1.0);
        assertThat(first.at("/components/d").asDouble()).isEqualTo(1.0); // cùng chỗ, trong bán kính 200 m
        assertThat(first.get("score").asDouble()).isGreaterThan(res.at("/results/1/score").asDouble());
        assertThat(texts(first.get("reasons"))).contains("Ảnh/mô tả rất giống", "Trong khu vực bạn mất", "Cùng danh mục");
        assertThat(texts(res.at("/results/2/reasons"))).contains("Đang xử lý ảnh, điểm có thể chưa chính xác");
        assertThat(first.at("/item/thumbUrl").asText()).startsWith("https://storage.test/");
        assertThat(first.at("/item/exactLocation").isMissingNode()).isTrue();
        assertThat(gemini.calls).hasSize(1);
    }

    @Test
    void onlyOpenVisibleItemsOfRequestedTypeAndModel() throws Exception {
        UUID ok = embedded(items.found(a).category("earphones").at(spot[0], spot[1]).insert(), 0.5, 1);
        embedded(items.found(a).category("earphones").status("claimed").at(spot[0], spot[1]).insert(), 0.9, 1);
        embedded(items.found(a).category("earphones").status("under_review").at(spot[0], spot[1]).insert(), 0.9, 1);
        embedded(items.found(a).category("earphones").deleted().at(spot[0], spot[1]).insert(), 0.9, 1);
        embedded(items.lost(a).category("earphones").at(spot[0], spot[1]).insert(), 0.9, 1);
        UUID otherModel = items.found(a).category("earphones").at(spot[0], spot[1]).insert();
        insertVector(otherModel, "model-khac", FakeEmbeddingClient.withCosine(0.9, 1));

        JsonNode res = search(b, query("found")).andExpect(status().isOk()).andReturn().json();
        assertThat(ids(res)).containsExactly(ok, otherModel); // vector của model khác không được dùng (7.3)
        assertThat(res.at("/results/1/components/v").asDouble()).isZero();
    }

    /** Lọc cứng 4.2: xa hơn 3 km; nhặt trước lúc mất quá 24 giờ. */
    @Test
    void hardFiltersDistanceAndTime() throws Exception {
        Instant lostAt = Instant.parse("2026-11-20T02:00:00Z");
        UUID near = embedded(items.found(a).at(spot[0], spot[1]).happenedAt(lostAt.plus(2, ChronoUnit.HOURS)).insert(), 0.4, 1);
        embedded(items.found(a).at(spot[0] + 0.05, spot[1]).happenedAt(lostAt.plus(2, ChronoUnit.HOURS)).insert(), 0.9, 1); // ~5,5 km
        UUID slightlyEarlier = embedded(items.found(a).at(spot[0], spot[1]).happenedAt(lostAt.minus(10, ChronoUnit.HOURS)).insert(), 0.3, 1);
        embedded(items.found(a).at(spot[0], spot[1]).happenedAt(lostAt.minus(30, ChronoUnit.HOURS)).insert(), 0.9, 1);

        Map<String, Object> q = query("found");
        q.put("happenedAt", "2026-11-20T09:00:00+07:00");
        JsonNode res = search(b, q).andExpect(status().isOk()).andReturn().json();
        assertThat(ids(res)).containsExactlyInAnyOrder(near, slightlyEarlier);
        JsonNode nearResult = res.at("/results/0");
        assertThat(texts(nearResult.get("reasons"))).contains("Nhặt 2 giờ sau khi mất");
    }

    /** Khoảng cách tính tới vị trí công khai, không lộ vị trí chính xác qua lý do (BR-11). */
    @Test
    void distanceUsesPublicLocation() throws Exception {
        double lat = spot[0] + 0.0004; // vị trí công khai là spot[0], cách vị trí chính xác ~44 m
        embedded(items.found(a).at(lat, spot[1]).insert(), 0.5, 1);
        Map<String, Object> q = query("found");
        q.put("location", Map.of("lat", lat, "lng", spot[1]));
        q.put("radiusM", 0);
        JsonNode res = search(b, q).andExpect(status().isOk()).andReturn().json();
        // nếu đo tới vị trí chính xác thì d = 0 và s_d = 1
        assertThat(res.at("/results/0/components/d").asDouble()).isLessThan(1.0).isGreaterThan(0.8);
    }

    @Test
    void sensitiveResultHasNoThumb() throws Exception {
        embedded(items.found(a).category("student_card").at(spot[0], spot[1]).insert(), 0.5, 1);
        JsonNode res = search(b, query("found")).andExpect(status().isOk()).andReturn().json();
        assertThat(res.at("/results/0/item/thumbUrl").isNull()).isTrue();
    }

    @Test
    void queryVectorIsCached() throws Exception {
        search(b, query("found")).andExpect(status().isOk());
        search(b, query("found")).andExpect(status().isOk());
        assertThat(gemini.calls).hasSize(1);
        // cache sống qua lần khởi động lại: Gemini lỗi mà câu đã có trong cache thì không hạ cấp
        gemini.failWith = new EmbeddingException("500", false, null);
        JsonNode res = search(b, query("found")).andExpect(status().isOk()).andReturn().json();
        assertThat(res.get("degraded").asBoolean()).isFalse();
    }

    /** 4.3: Gemini lỗi → lọc từ khóa trên mô tả, {@code degraded=true}. */
    @Test
    void geminiFailureDegradesToKeywords() throws Exception {
        UUID match = items.found(a).description("Tai nghe màu trắng").at(spot[0], spot[1]).insert();
        items.found(a).description("Ví da nâu").at(spot[0], spot[1]).insert();
        gemini.failWith = new EmbeddingException("500", false, null);

        Map<String, Object> q = query("found");
        q.put("text", "tai nghe 50%_");
        JsonNode res = search(b, q).andExpect(status().isOk()).andReturn().json();
        assertThat(res.get("degraded").asBoolean()).isTrue();
        assertThat(ids(res)).containsExactly(match);
        assertThat(circuit.isOpen()).isFalse();
    }

    /** 7.3: 429 → tạm dừng; trong lúc tạm dừng không gọi Gemini nữa. */
    @Test
    void rateLimitPausesAndDegrades() throws Exception {
        gemini.failWith = new EmbeddingException("429", true, null);
        search(b, query("found")).andExpect(status().isOk()).andExpect(jsonPath("$.degraded").value(true));
        assertThat(circuit.isOpen()).isTrue();
        gemini.failWith = null;
        Map<String, Object> other = query("found");
        other.put("text", "ví da nâu");
        search(b, other).andExpect(status().isOk()).andExpect(jsonPath("$.degraded").value(true));
        assertThat(gemini.calls).hasSize(1);
    }

    @Test
    void imageOnlySearchFailsWith503WhenAiIsDown() throws Exception {
        gemini.failWith = new EmbeddingException("500", false, null);
        Map<String, Object> q = query("found");
        q.remove("text");
        MockMultipartHttpServletRequestBuilder req = request(b, q);
        req.file(new MockMultipartFile("image", "q.jpg", "image/jpeg", TestItems.JPEG));
        mvc.perform(req).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"));
    }

    @Test
    void imageAndTextAreBothUsed() throws Exception {
        UUID id = embedded(items.found(a).at(spot[0], spot[1]).insert(), 0.3, 1);
        MockMultipartHttpServletRequestBuilder req = request(b, query("found"));
        req.file(new MockMultipartFile("image", "q.jpg", "image/jpeg", TestItems.JPEG));
        String body = mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(ids(json.readTree(body))).containsExactly(id);
        assertThat(gemini.calls.getFirst()).hasSize(2);
    }

    @Test
    void validation() throws Exception {
        Map<String, Object> bad = new LinkedHashMap<>();
        bad.put("type", "x");
        bad.put("text", "a".repeat(501));
        bad.put("radiusM", 5000);
        bad.put("category", "khong-co");
        bad.put("happenedAt", "2099-01-01T00:00:00+07:00");
        search(b, bad).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "type", "text", "radiusM", "happenedAt", "category")));

        MockMultipartHttpServletRequestBuilder png = request(b, query("found"));
        png.file(new MockMultipartFile("image", "q.png", "image/png", "\u0089PNG".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)));
        mvc.perform(png).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("image"));

        MockMultipartHttpServletRequestBuilder anon = multipart("/api/v1/search");
        anon.part(new MockPart("data", json.writeValueAsBytes(query("found"))));
        mvc.perform(anon).andExpect(status().isUnauthorized());
    }

    // --- hỗ trợ ---

    private Map<String, Object> query(String type) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("type", type);
        q.put("text", QUERY);
        q.put("location", Map.of("lat", spot[0], "lng", spot[1]));
        q.put("radiusM", 200);
        q.put("category", "earphones");
        return q;
    }

    private Search search(TestUser user, Map<String, Object> data) throws Exception {
        return new Search(mvc.perform(request(user, data)));
    }

    private MockMultipartHttpServletRequestBuilder request(TestUser user, Map<String, Object> data) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/v1/search");
        req.part(new MockPart("data", json.writeValueAsBytes(data)));
        req.header("Authorization", user.bearer());
        return req;
    }

    /** Ghi vector chữ của tin với cosine {@code cos} so với câu truy vấn, đánh dấu {@code done}. */
    private UUID embedded(UUID id, double cos, int axis) {
        insertVector(id, FakeEmbeddingClient.MODEL, FakeEmbeddingClient.withCosine(cos, axis));
        jdbc.update("update refound.items set embedding_status = 'done' where id = ?", id);
        return id;
    }

    private void insertVector(UUID id, String model, float[] v) {
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            s.append(i == 0 ? "" : ",").append(v[i]);
        }
        jdbc.update("insert into refound.item_embeddings (item_id, source, model, embedding) values (?, 'text', ?, cast(? as vector))",
                id, model, s.append("]").toString());
    }

    private static List<UUID> ids(JsonNode res) {
        List<UUID> ids = new ArrayList<>();
        res.get("results").forEach(r -> ids.add(UUID.fromString(r.at("/item/id").asText())));
        return ids;
    }

    private static List<String> texts(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.asText()));
        return out;
    }

    /** Bọc ResultActions để đọc JSON gọn. */
    private final class Search {

        private final ResultActions actions;

        Search(ResultActions actions) {
            this.actions = actions;
        }

        Search andExpect(org.springframework.test.web.servlet.ResultMatcher m) throws Exception {
            actions.andExpect(m);
            return this;
        }

        Search andReturn() {
            return this;
        }

        JsonNode json() throws Exception {
            return json.readTree(actions.andReturn().getResponse().getContentAsString());
        }
    }
}
