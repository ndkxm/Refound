package com.refound.item.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.support.IntegrationTest;
import com.refound.support.TestItems;
import com.refound.support.TestUsers;
import com.refound.support.TestUsers.TestUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** {@code GET /items} (FR-ITEM-03) và {@code GET /me/items} (FR-ITEM-05): lọc, ẩn tin, phân trang. */
@IntegrationTest
class ItemListIT {

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

    @Test
    void publicListShowsOnlyOpenItemsInBox() throws Exception {
        TestUser a = users.a();
        double[] p = TestItems.randomSpot();
        UUID lost = items.lost(a).at(p[0], p[1]).insert();
        UUID found = items.found(a).at(p[0], p[1]).insert();
        items.found(a).at(p[0], p[1]).status("claimed").insert();
        items.found(a).at(p[0], p[1]).status("under_review").insert();
        items.lost(a).at(p[0], p[1]).status("closed").insert();
        items.lost(a).at(p[0], p[1]).deleted().insert();

        assertThat(ids(list(users.b(), box(p)))).containsExactlyInAnyOrder(lost, found);
        assertThat(ids(list(users.b(), box(p) + "&type=lost"))).containsExactly(lost);
        assertThat(ids(list(users.b(), box(p) + "&category=bag"))).isEmpty();
    }

    @Test
    void sensitiveItemsHaveNoThumbInList() throws Exception {
        double[] p = TestItems.randomSpot();
        UUID card = items.found(users.a()).at(p[0], p[1]).category("bank_card").insert();
        JsonNode page = list(users.b(), box(p));
        assertThat(page.at("/items/0/id").asText()).isEqualTo(card.toString());
        assertThat(page.at("/items/0/thumbUrl").isNull()).isTrue();
        assertThat(page.at("/items/0/sensitive").asBoolean()).isTrue();
        assertThat(page.at("/items/0/exactLocation").isMissingNode()).isTrue();
    }

    /** Ghi chú P0-01: khung lọc theo vị trí công khai, không theo vị trí chính xác (BR-11). */
    @Test
    void bboxUsesPublicLocation() throws Exception {
        double[] p = TestItems.randomSpot(); // 3 chữ số thập phân
        double lat = p[0] + 0.0004; // vị trí công khai làm tròn về p[0], thấp hơn 0,0004 độ (~44 m)
        UUID found = items.found(users.a()).at(lat, p[1]).insert();
        // khung chỉ chứa vị trí chính xác, không chứa vị trí công khai
        String exactOnly = "bbox=" + (p[1] - 0.01) + "," + (lat - 0.0001) + "," + (p[1] + 0.01) + "," + (lat + 0.0001);
        assertThat(ids(list(users.b(), exactOnly))).doesNotContain(found);
        assertThat(ids(list(users.b(), box(p)))).contains(found);
    }

    @Test
    void timeRangeFilter() throws Exception {
        double[] p = TestItems.randomSpot();
        Instant t = Instant.parse("2026-11-10T02:00:00Z");
        UUID early = items.lost(users.a()).at(p[0], p[1]).happenedAt(t).insert();
        UUID late = items.lost(users.a()).at(p[0], p[1]).happenedAt(t.plus(2, ChronoUnit.DAYS)).insert();
        assertThat(ids(list(users.b(), box(p) + "&from=2026-11-11T09:00:00%2B07:00"))).containsExactly(late);
        assertThat(ids(list(users.b(), box(p) + "&to=2026-11-11T09:00:00%2B07:00"))).containsExactly(early);
        mvc.perform(get("/api/v1/items?from=2026-11-12T00:00:00Z&to=2026-11-11T00:00:00Z")
                        .header("Authorization", users.b().bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagesDoNotOverlapAndEndWithNullCursor() throws Exception {
        double[] p = TestItems.randomSpot();
        Instant base = Instant.now().minus(10, ChronoUnit.HOURS);
        List<UUID> expected = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            expected.add(0, items.lost(users.a()).at(p[0], p[1]).happenedAt(base.plus(i, ChronoUnit.HOURS)).insert());
        }
        // hai tin cùng thời điểm: thứ tự phụ theo id, vẫn không lặp/không sót
        Instant same = base.plus(1, ChronoUnit.MINUTES);
        UUID tie1 = items.lost(users.a()).at(p[0], p[1]).happenedAt(same).insert();
        UUID tie2 = items.lost(users.a()).at(p[0], p[1]).happenedAt(same).insert();

        List<UUID> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            JsonNode page = list(users.b(), box(p) + "&limit=2" + (cursor == null ? "" : "&cursor=" + cursor));
            seen.addAll(ids(page));
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asText();
            pages++;
        } while (cursor != null);
        assertThat(pages).isEqualTo(4);
        assertThat(seen).hasSize(7).doesNotHaveDuplicates().containsAll(expected).contains(tie1, tie2);
        assertThat(seen.subList(0, 4)).containsExactlyElementsOf(expected.subList(0, 4)); // mới nhất trước
    }

    @Test
    void badQueryParamsAre400() throws Exception {
        TestUser b = users.b();
        for (String q : List.of("bbox=1,2,3", "bbox=10,0,5,1", "bbox=a,b,c,d", "limit=0", "limit=51", "type=x",
                "cursor=khong-hop-le", "from=hom-qua")) {
            mvc.perform(get("/api/v1/items?" + q).header("Authorization", b.bearer()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void myItemsShowAllMyStatusesExceptDeleted() throws Exception {
        TestUser a = users.a();
        UUID open = items.lost(a).insert();
        UUID review = items.found(a).status("under_review").insert();
        UUID closed = items.lost(a).status("closed").insert();
        items.lost(a).deleted().insert();
        items.lost(users.b()).insert();

        JsonNode page = fetch(a, "/api/v1/me/items");
        assertThat(ids(page)).containsExactlyInAnyOrder(open, review, closed);
        assertThat(page.at("/items/0/exactLocation").isObject()).isTrue();
        assertThat(ids(fetch(a, "/api/v1/me/items?status=closed"))).containsExactly(closed);
        mvc.perform(get("/api/v1/me/items?status=lung-tung").header("Authorization", a.bearer()))
                .andExpect(status().isBadRequest());
    }

    private JsonNode list(TestUser user, String query) throws Exception {
        return fetch(user, "/api/v1/items?" + query);
    }

    private JsonNode fetch(TestUser user, String url) throws Exception {
        String body = mvc.perform(get(java.net.URI.create(url))
                        .header("Authorization", user.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private static String box(double[] p) {
        return "bbox=" + (p[1] - 0.01) + "," + (p[0] - 0.01) + "," + (p[1] + 0.01) + "," + (p[0] + 0.01);
    }

    private static List<UUID> ids(JsonNode page) {
        List<UUID> ids = new ArrayList<>();
        page.get("items").forEach(n -> ids.add(UUID.fromString(n.get("id").asText())));
        return ids;
    }
}
