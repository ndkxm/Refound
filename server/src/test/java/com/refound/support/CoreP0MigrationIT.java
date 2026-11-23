package com.refound.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** P0-01: migration V202611230900__core_p0 chạy được, dữ liệu cố định và ràng buộc chính đúng như 02 §2.4. */
@IntegrationTest

@Transactional // mỗi test rollback, không để dữ liệu lại cho test khác
class CoreP0MigrationIT {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void categoriesSeeded() {
        assertThat(jdbc.queryForObject("select count(*) from refound.categories", Integer.class)).isEqualTo(14);
        assertThat(jdbc.queryForObject(
                        "select count(*) from refound.categories where sensitive", Integer.class))
                .isEqualTo(3);
    }

    @Test
    void foundItemWithEmbeddingInserts() {
        UUID item = insertItem("found", null);
        jdbc.update(
                "insert into refound.item_embeddings (item_id, source, model, embedding) values (?, 'text', 'm', ?::extensions.vector)",
                item,
                zeroVector(768));
        assertThat(jdbc.queryForObject("select count(*) from refound.item_embeddings", Integer.class)).isEqualTo(1);
    }

    // Mỗi ca một transaction riêng: Postgres hủy cả transaction sau câu lỗi đầu tiên.
    @ParameterizedTest
    @CsvSource({"lost,", "lost,30", "lost,2001", "found,100"})
    void radiusRuleRejects(String type, Integer radius) {
        assertThatThrownBy(() -> insertItem(type, radius)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void radiusRuleAcceptsLostWithinRange() {
        assertThat(insertItem("lost", 50)).isNotNull();
        assertThat(insertItem("lost", 2000)).isNotNull();
    }

    @Test
    void claimedOnlyForFound() {
        UUID found = insertItem("found", null);
        assertThat(jdbc.update("update refound.items set status = 'claimed' where id = ?", found)).isEqualTo(1);
    }

    @Test
    void claimedLostRejected() {
        UUID lost = insertItem("lost", 100);
        assertThatThrownBy(() -> jdbc.update("update refound.items set status = 'claimed' where id = ?", lost))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void closedRequiresReason() {
        UUID item = insertItem("found", null);
        assertThat(jdbc.update(
                        "update refound.items set status = 'closed', closed_reason = 'Đã trả trực tiếp' where id = ?", item))
                .isEqualTo(1);
    }

    @Test
    void closedWithoutReasonRejected() {
        UUID item = insertItem("found", null);
        assertThatThrownBy(() -> jdbc.update("update refound.items set status = 'closed' where id = ?", item))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateTextEmbeddingRejected() {
        UUID item = insertItem("found", null);
        String sql =
                "insert into refound.item_embeddings (item_id, source, model, embedding) values (?, 'text', 'm', ?::extensions.vector)";
        jdbc.update(sql, item, zeroVector(768));
        assertThatThrownBy(() -> jdbc.update(sql, item, zeroVector(768)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void embeddingDimensionIs768() {
        assertThatThrownBy(() -> jdbc.update(
                        "insert into refound.embedding_cache (key, embedding) values ('k', ?::extensions.vector)",
                        zeroVector(767)))
                .isInstanceOf(DataAccessException.class);
    }

    private UUID insertItem(String type, Integer radius) {
        UUID user = jdbc.queryForObject(
                "insert into refound.users (firebase_uid, display_name) values (gen_random_uuid()::text, 'Test') returning id",
                UUID.class);
        String point = "extensions.st_geogfromtext('POINT(106.803128 10.870345)')";
        return jdbc.queryForObject(
                "insert into refound.items (author_id, type, category, description, happened_at, location, public_location, radius_m) "
                        + "values (?, ?, 'wallet', 'Ví da màu nâu', now(), " + point + ", " + point + ", ?) returning id",
                UUID.class,
                user,
                type,
                radius);
    }

    private static String zeroVector(int dim) {
        return "[" + "0,".repeat(dim - 1) + "0]";
    }
}
