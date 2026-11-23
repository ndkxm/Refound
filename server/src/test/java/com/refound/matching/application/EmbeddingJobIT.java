package com.refound.matching.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.refound.item.application.ItemContentChanged;
import com.refound.matching.infrastructure.EmbeddingClient.EmbeddingException;
import com.refound.matching.infrastructure.EmbeddingClient.Input;
import com.refound.support.FakeEmbeddingClient;
import com.refound.support.FakeVisionClient;
import com.refound.support.IntegrationTest;
import com.refound.support.TestItems;
import com.refound.support.TestUsers;
import com.refound.support.TestUsers.TestUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * P0-10: hàng đợi embedding trên {@code items} (04 §4.1, 4.4, 7.3) với Gemini và Groq giả. Luồng nền tắt trong test
 * ({@code refound.embedding.enabled=false}); test gọi {@link EmbeddingJob#runOnce()} trực tiếp.
 */
@IntegrationTest
@RecordApplicationEvents
class EmbeddingJobIT {

    @Autowired
    EmbeddingJob job;

    @Autowired
    AiCircuit circuit;

    @Autowired
    FakeEmbeddingClient gemini;

    @Autowired
    FakeVisionClient groq;

    @Autowired
    TestUsers users;

    @Autowired
    TestItems items;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEvents events;

    @BeforeEach
    void isolate() {
        // tin pending do test khác để lại không chen vào lô của test này
        jdbc.update("update refound.items set next_embed_at = 'infinity' where embedding_status = 'pending'");
        gemini.reset();
        groq.reset();
        circuit.reset();
    }

    @AfterEach
    void cleanUp() {
        gemini.reset();
        groq.reset();
        circuit.reset();
    }

    @Test
    void foundItemGetsCaptionTextAndImageVectors() {
        UUID id = due(items.found(users.a()).images(2).insert());

        assertThat(job.runOnce()).isEqualTo(1);

        assertThat(row(id)).containsEntry("embedding_status", "done").containsEntry("ai_caption", FakeVisionClient.DEFAULT_CAPTION);
        assertThat(groq.calls).hasSize(1);
        assertThat(groq.calls.getFirst()).hasSize(2);
        List<Input> sent = gemini.calls.getFirst();
        assertThat(sent).hasSize(3);
        assertThat(((Input.Text) sent.get(0)).text())
                .isEqualTo("title: Ví | text: Ví da nâu. " + FakeVisionClient.DEFAULT_CAPTION);
        assertThat(sent.get(1)).isInstanceOf(Input.Image.class);
        assertThat(vectors(id)).containsExactlyInAnyOrder("text|false|" + FakeEmbeddingClient.MODEL,
                "image|true|" + FakeEmbeddingClient.MODEL, "image|true|" + FakeEmbeddingClient.MODEL);
        assertThat(jdbc.queryForObject("select vector_dims(embedding) from refound.item_embeddings where item_id = ? limit 1",
                Integer.class, id)).isEqualTo(768);
    }

    /** BR-08: tin nhạy cảm không gửi ảnh cho Groq lẫn Gemini, chỉ có vector chữ. */
    @Test
    void sensitiveItemSendsNoImagesToAnyAi() {
        UUID id = due(items.found(users.a()).category("student_card").images(2).insert());

        job.runOnce();

        assertThat(row(id)).containsEntry("embedding_status", "done").containsEntry("ai_caption", null);
        assertThat(groq.calls).isEmpty();
        assertThat(gemini.calls.getFirst()).hasSize(1).allMatch(in -> in instanceof Input.Text);
        assertThat(vectors(id)).containsExactly("text|false|" + FakeEmbeddingClient.MODEL);
    }

    @Test
    void lostItemWithoutImagesSkipsGroq() {
        UUID id = due(items.lost(users.a()).insert());
        job.runOnce();
        assertThat(row(id)).containsEntry("embedding_status", "done");
        assertThat(groq.calls).isEmpty();
    }

    /** ADR-02: Groq lỗi thì bỏ mô tả, vẫn done. */
    @Test
    void groqFailureStillEmbeds() {
        groq.caption = Optional.empty();
        UUID id = due(items.found(users.a()).insert());
        job.runOnce();
        assertThat(row(id)).containsEntry("embedding_status", "done").containsEntry("ai_caption", null);
        assertThat(((Input.Text) gemini.calls.getFirst().getFirst()).text()).isEqualTo("title: Ví | text: Ví da nâu");
    }

    @Test
    void existingCaptionIsReusedWithoutCallingGroq() {
        UUID id = due(items.found(users.a()).insert());
        jdbc.update("update refound.items set ai_caption = 'mô tả cũ' where id = ?", id);
        job.runOnce();
        assertThat(groq.calls).isEmpty();
        assertThat(((Input.Text) gemini.calls.getFirst().getFirst()).text()).endsWith(". mô tả cũ");
    }

    /** 7.3: Gemini lỗi → pending, lần lỗi +1, thử lại sau 1 phút; mô tả ảnh vừa có vẫn được lưu. */
    @Test
    void geminiFailureBacksOff() {
        gemini.failWith = new EmbeddingException("500", false, null);
        UUID id = due(items.found(users.a()).insert());

        job.runOnce();

        Map<String, Object> r = row(id);
        assertThat(r).containsEntry("embedding_status", "pending").containsEntry("embedding_attempts", 1)
                .containsEntry("ai_caption", FakeVisionClient.DEFAULT_CAPTION);
        assertThat((Double) r.get("wait_s")).isBetween(50.0, 70.0);
        assertThat(vectors(id)).isEmpty();
        assertThat(circuit.isOpen()).isFalse();
        assertThat(job.runOnce()).isZero(); // chưa đến hạn
    }

    @Test
    void seventhFailureMarksFailed() {
        gemini.failWith = new EmbeddingException("500", false, null);
        UUID id = due(items.lost(users.a()).insert());
        jdbc.update("update refound.items set embedding_attempts = 6 where id = ?", id);
        job.runOnce();
        assertThat(row(id)).containsEntry("embedding_status", "failed").containsEntry("embedding_attempts", 7);
    }

    /** 7.3: 429 → tạm dừng 5 phút; tin còn lại trong lô trả về hàng đợi không tính lỗi. */
    @Test
    void rateLimitPausesEverything() {
        gemini.failWith = new EmbeddingException("429", true, null);
        UUID first = due(items.lost(users.a()).insert());
        UUID second = due(items.lost(users.a()).insert());

        job.runOnce();

        assertThat(circuit.isOpen()).isTrue();
        assertThat(gemini.calls).hasSize(1);
        assertThat(List.of(row(first).get("embedding_attempts"), row(second).get("embedding_attempts")))
                .containsExactlyInAnyOrder(1, 0);
        assertThat(job.runOnce()).isZero(); // đang tạm dừng: không nhận việc
        assertThat(gemini.calls).hasSize(1);
    }

    @Test
    void storageFailureCountsAsFailure() {
        UUID id = due(items.found(users.a()).insert());
        jdbc.update("update refound.item_images set thumb_path = 'khong-ton-tai.jpg' where item_id = ?", id);
        job.runOnce();
        assertThat(row(id)).containsEntry("embedding_status", "pending").containsEntry("embedding_attempts", 1);
        assertThat(gemini.calls).isEmpty();
    }

    /** Tin bị sửa trong lúc chờ AI: bỏ kết quả cũ, tin vẫn pending cho lần sau (I2). */
    @Test
    void editDuringEmbeddingDiscardsStaleResult() {
        UUID id = due(items.lost(users.a()).insert());
        gemini.during = () -> jdbc.update(
                "update refound.items set description = 'Mô tả mới', updated_at = now() + interval '1 second', "
                        + "next_embed_at = now() where id = ?", id);

        job.runOnce();

        assertThat(row(id)).containsEntry("embedding_status", "pending");
        assertThat(vectors(id)).isEmpty();
        job.runOnce();
        assertThat(row(id)).containsEntry("embedding_status", "done");
        assertThat(((Input.Text) gemini.calls.getLast().getFirst()).text()).endsWith("Mô tả mới");
    }

    @Test
    void reembedReplacesOldVectors() {
        UUID id = due(items.found(users.a()).insert());
        job.runOnce();
        assertThat(vectors(id)).hasSize(2);
        jdbc.update("update refound.items set embedding_status = 'pending', next_embed_at = now() where id = ?", id);
        job.runOnce();
        assertThat(vectors(id)).hasSize(2); // không trùng, không vi phạm unique (item_id, source, image_id)
    }

    @Test
    void onlyDueOpenUndeletedItemsAreClaimed() {
        TestUser a = users.a();
        due(items.lost(a).status("closed").insert());
        due(items.lost(a).deleted().insert());
        UUID later = items.lost(a).insert();
        jdbc.update("update refound.items set next_embed_at = now() + interval '1 hour' where id = ?", later);
        assertThat(job.runOnce()).isZero();
        assertThat(gemini.calls).isEmpty();
    }

    /** I1, I2: tạo tin và sửa mô tả phát sự kiện để job chạy ngay sau commit; sửa giờ thì không. */
    @Test
    void createAndContentEditPublishEvent() throws Exception {
        TestUser a = users.a();
        String data = "{\"type\":\"lost\",\"category\":\"keys\",\"description\":\"Chìa khóa xe máy\","
                + "\"happenedAt\":\"2026-11-20T09:00:00+07:00\",\"location\":{\"lat\":10.87,\"lng\":106.8},\"radiusM\":100}";
        MockMultipartHttpServletRequestBuilder create = multipart("/api/v1/items");
        create.part(new MockPart("data", data.getBytes(StandardCharsets.UTF_8)));
        mvc.perform(create.header("Authorization", a.bearer())).andExpect(status().isCreated());
        assertThat(events.stream(ItemContentChanged.class)).hasSize(1);

        UUID id = items.lost(a).insert();
        mvc.perform(patch("/api/v1/items/" + id).header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"happenedAt\":\"2026-11-20T09:00:00+07:00\"}"))
                .andExpect(status().isOk());
        assertThat(events.stream(ItemContentChanged.class)).hasSize(1);
        mvc.perform(patch("/api/v1/items/" + id).header("Authorization", a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Mô tả khác hẳn\"}"))
                .andExpect(status().isOk());
        assertThat(events.stream(ItemContentChanged.class)).hasSize(2).contains(new ItemContentChanged(id));
    }

    // --- hỗ trợ ---

    private UUID due(UUID id) {
        jdbc.update("update refound.items set next_embed_at = now() - interval '1 second' where id = ?", id);
        return id;
    }

    private Map<String, Object> row(UUID id) {
        return jdbc.queryForMap("select embedding_status, embedding_attempts, ai_caption, "
                + "extract(epoch from next_embed_at - now())::float8 as wait_s from refound.items where id = ?", id);
    }

    private List<String> vectors(UUID id) {
        return jdbc.query("select source, image_id is not null as has_image, model from refound.item_embeddings where item_id = ?",
                (rs, i) -> rs.getString(1) + "|" + rs.getBoolean(2) + "|" + rs.getString(3), id);
    }
}
