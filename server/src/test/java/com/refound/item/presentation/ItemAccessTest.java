package com.refound.item.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.support.IntegrationTest;
import com.refound.support.TestItems;
import com.refound.support.TestUsers;
import com.refound.support.TestUsers.TestUser;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Test truy cập chéo X1–X6 (03 §3.4) và chuyển trạng thái tin qua API (05 §5.1: I2, I6, I7, I9, I13).
 * A là tác giả, B là người lạ.
 */
@IntegrationTest
class ItemAccessTest {

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

    // --- X1, X2: người lạ sửa/đóng/gia hạn/xóa tin của A ---

    @Test
    void x1StrangerCannotEdit() throws Exception {
        TestUser a = users.a();
        UUID id = items.lost(a).insert();
        forbidden(call(users.b(), patch(url(id)).contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Đổi của người khác\"}")));
        assertThat(description(id)).isEqualTo("Ví da nâu");
    }

    @Test
    void x2StrangerCannotCloseRenewMarkFoundOrDelete() throws Exception {
        TestUser a = users.a();
        TestUser b = users.b();
        UUID open = items.lost(a).insert();
        forbidden(call(b, post(url(open) + "/close").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}")));
        forbidden(call(b, post(url(open) + "/mark-found")));
        forbidden(call(b, delete(url(open))));
        // tin hết hạn của A thì B không được biết là có → 404 (03 §3.3)
        UUID expired = items.lost(a).status("expired").insert();
        notFound(call(b, post(url(expired) + "/renew")));
        assertThat(jdbc.queryForObject("select status from refound.items where id = ?", String.class, open)).isEqualTo("open");
        assertThat(jdbc.queryForObject("select deleted_at is null from refound.items where id = ?", Boolean.class, open)).isTrue();
    }

    @Test
    void adminCannotEditButSeesFullItem() throws Exception {
        TestUser a = users.a();
        TestUser admin = users.admin();
        UUID id = items.found(a).at(10.8705, 106.8035).insert();
        forbidden(call(admin, patch(url(id)).contentType(MediaType.APPLICATION_JSON).content("{}")));
        call(admin, get(url(id))).andExpect(status().isOk()).andExpect(jsonPath("$.exactLocation.lat").value(10.8705));
    }

    // --- X3: B xem tin found của A ---

    @Test
    void x3StrangerSeesRoundedLocationWithoutSecret() throws Exception {
        TestUser a = users.a();
        UUID id = items.found(a).at(10.8705, 106.8035).insert();
        String body = call(users.b(), get(url(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicLocation.lat").value(10.871))
                .andExpect(jsonPath("$.publicLocation.lng").value(106.804))
                .andExpect(jsonPath("$.images.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> fields = json.readValue(body, new TypeReference<>() {});
        assertThat(fields).doesNotContainKeys("exactLocation", "answer", "question", "embeddingStatus", "closedReason");
        assertThat(body).doesNotContain("10.8705").doesNotContain("Thẻ xe");

        call(a, get(url(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exactLocation.lat").value(10.8705))
                .andExpect(jsonPath("$.answer").doesNotExist());
    }

    // --- X4: tin under_review / đã xóa ---

    @Test
    void x4HiddenItemsAre404ForStranger() throws Exception {
        TestUser a = users.a();
        TestUser b = users.b();
        UUID review = items.found(a).status("under_review").insert();
        UUID deleted = items.found(a).deleted().insert();
        UUID closed = items.lost(a).status("closed").insert();
        notFound(call(b, get(url(review))));
        notFound(call(b, get(url(deleted))));
        notFound(call(b, get(url(closed))));
        notFound(call(b, get(url(UUID.randomUUID()))));
        // tác giả thấy tin under_review của mình, nhưng tin đã xóa thì không; admin thấy cả hai
        call(a, get(url(review))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("under_review"));
        notFound(call(a, get(url(deleted))));
        call(users.admin(), get(url(deleted))).andExpect(status().isOk());
    }

    @Test
    void claimedItemStillVisibleToStrangers() throws Exception {
        UUID id = items.found(users.a()).status("claimed").insert();
        call(users.b(), get(url(id))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("claimed"));
    }

    // --- X5: tin nhạy cảm ---

    @Test
    void x5SensitiveItemHasNoImagesForStranger() throws Exception {
        TestUser a = users.a();
        UUID id = items.found(a).category("student_card").images(2).insert();
        call(users.b(), get(url(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensitive").value(true))
                .andExpect(jsonPath("$.images.length()").value(0));
        call(a, get(url(id))).andExpect(jsonPath("$.images.length()").value(2));
    }

    // --- X6: đáp án câu hỏi kín ---

    @Test
    void x6SecretOnlyForFoundAuthor() throws Exception {
        TestUser a = users.a();
        UUID found = items.found(a).insert();
        call(a, get(url(found) + "/secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("Trong ví có gì?"))
                .andExpect(jsonPath("$.answer").value("Thẻ xe"));
        notFound(call(users.b(), get(url(found) + "/secret")));
        notFound(call(users.admin(), get(url(found) + "/secret")));
        notFound(call(a, get(url(items.lost(a).insert()) + "/secret")));
    }

    // --- I2: sửa ---

    @Test
    void editDescriptionRequeuesEmbeddingAndDropsOldVectors() throws Exception {
        TestUser a = users.a();
        UUID id = items.lost(a).insert();
        jdbc.update("update refound.items set embedding_status = 'done', embedding_attempts = 3, ai_caption = 'cũ' where id = ?", id);
        jdbc.update("insert into refound.item_embeddings (item_id, source, model, embedding) values (?, 'text', 'm', array_fill(0.1, array[768])::vector)", id);

        call(a, patch(url(id)).contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"  Ví da đen, có móc khóa  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Ví da đen, có móc khóa"))
                .andExpect(jsonPath("$.embeddingStatus").value("pending"));
        Map<String, Object> row = jdbc.queryForMap(
                "select embedding_attempts, ai_caption, (select count(*) from refound.item_embeddings e where e.item_id = i.id) n "
                        + "from refound.items i where id = ?", id);
        assertThat(row).containsEntry("embedding_attempts", 0).containsEntry("ai_caption", "cũ").containsEntry("n", 0L);
    }

    @Test
    void editTimeOrLocationKeepsEmbeddingButRecomputesPublicLocation() throws Exception {
        TestUser a = users.a();
        UUID id = items.found(a).insert();
        jdbc.update("update refound.items set embedding_status = 'done' where id = ?", id);
        call(a, patch(url(id)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":{\"lat\":10.8705,\"lng\":106.8035},\"happenedAt\":\"2026-11-20T09:00:00+07:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embeddingStatus").value("done"))
                .andExpect(jsonPath("$.exactLocation.lat").value(10.8705))
                .andExpect(jsonPath("$.publicLocation.lat").value(10.871))
                .andExpect(jsonPath("$.happenedAt").value("2026-11-20T09:00:00+07:00"));
    }

    @Test
    void editToSensitiveCategoryClearsCaptionAndLostWithImagesIsRejected() throws Exception {
        TestUser a = users.a();
        UUID found = items.found(a).insert();
        jdbc.update("update refound.items set ai_caption = 'ví nâu có logo' where id = ?", found);
        call(a, patch(url(found)).contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"student_card\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensitive").value(true));
        assertThat(jdbc.queryForObject("select ai_caption from refound.items where id = ?", String.class, found)).isNull();

        UUID lostWithImage = items.lost(a).images(1).insert();
        call(a, patch(url(lostWithImage)).contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"id_card\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("full"));
    }

    @Test
    void editValidatesMergedItem() throws Exception {
        TestUser a = users.a();
        UUID lost = items.lost(a).insert();
        call(a, patch(url(lost)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"  \",\"radiusM\":10,\"question\":\"Hỏi gì?\",\"location\":{\"lat\":10}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("location")); // vị trí thiếu lng báo trước
        call(a, patch(url(lost)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"  \",\"radiusM\":10,\"question\":\"Hỏi gì?\",\"category\":\"khong-co\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "category", "description", "radiusM", "question")));
        // tin mất: câu hỏi rỗng coi như không gửi
        call(a, patch(url(lost)).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"\",\"answer\":\" \"}"))
                .andExpect(status().isOk());
    }

    @Test
    void editFoundSecret() throws Exception {
        TestUser a = users.a();
        UUID id = items.found(a).insert();
        call(a, patch(url(id)).contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"Thẻ sinh viên\"}"))
                .andExpect(status().isOk());
        call(a, get(url(id) + "/secret"))
                .andExpect(jsonPath("$.question").value("Trong ví có gì?"))
                .andExpect(jsonPath("$.answer").value("Thẻ sinh viên"));
    }

    // --- I6, I7, I9, I13 ---

    @Test
    void closeThenEditIs409() throws Exception {
        TestUser a = users.a();
        UUID id = items.lost(a).insert();
        call(a, post(url(id) + "/close").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" Đã tìm được \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("closed"))
                .andExpect(jsonPath("$.closedReason").value("Đã tìm được"));
        invalidState(call(a, patch(url(id)).contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Sửa sau khi đóng\"}")));
        invalidState(call(a, post(url(id) + "/close").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"lần 2\"}")));
    }

    @Test
    void closeNeedsReason() throws Exception {
        TestUser a = users.a();
        UUID id = items.lost(a).insert();
        call(a, post(url(id) + "/close").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }

    @Test
    void markFoundOnlyForLost() throws Exception {
        TestUser a = users.a();
        call(a, post(url(items.lost(a).insert()) + "/mark-found"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("returned"));
        invalidState(call(a, post(url(items.found(a).insert()) + "/mark-found")));
    }

    @Test
    void renewExpiredResetsActivityAndWarning() throws Exception {
        TestUser a = users.a();
        UUID id = items.lost(a).status("expired").insert();
        jdbc.update("update refound.items set last_activity_at = now() - interval '40 days', expiry_warned_at = now() - interval '12 days' where id = ?", id);
        call(a, post(url(id) + "/renew")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("open"));
        Map<String, Object> row = jdbc.queryForMap(
                "select last_activity_at > now() - interval '1 minute' fresh, expiry_warned_at from refound.items where id = ?", id);
        assertThat(row).containsEntry("fresh", true).containsEntry("expiry_warned_at", null);
        invalidState(call(a, post(url(id) + "/renew"))); // đã open
    }

    @Test
    void deleteHidesEverywhereButClaimedCannotBeDeleted() throws Exception {
        TestUser a = users.a();
        UUID id = items.found(a).insert();
        call(a, delete(url(id))).andExpect(status().isNoContent());
        notFound(call(a, get(url(id))));
        notFound(call(a, delete(url(id))));
        notFound(call(users.b(), get(url(id))));

        invalidState(call(a, delete(url(items.found(a).status("claimed").insert()))));
        invalidState(call(a, delete(url(items.found(a).status("under_review").insert()))));
        call(a, delete(url(items.lost(a).status("returned").insert()))).andExpect(status().isNoContent());
    }

    // --- hỗ trợ ---

    private ResultActions call(TestUser user, MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req.header("Authorization", user.bearer()));
    }

    private static String url(UUID id) {
        return "/api/v1/items/" + id;
    }

    private String description(UUID id) {
        return jdbc.queryForObject("select description from refound.items where id = ?", String.class, id);
    }

    private static void forbidden(ResultActions r) throws Exception {
        r.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private static void notFound(ResultActions r) throws Exception {
        r.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private static void invalidState(ResultActions r) throws Exception {
        r.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }
}
