package com.refound.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.refound.support.TestUsers.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** R03, R04: migration V202611231130__moderation_blocks_terms (03 §3.5). */
@IntegrationTest
@Transactional // Postgres hủy cả transaction sau câu lỗi đầu tiên: mỗi ca lỗi một test
class ModerationMigrationIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestUsers users;

    @Test
    void blockInsertsOnce() {
        TestUser a = users.a();
        TestUser b = users.b();
        block(a, b);
        assertThat(jdbc.queryForObject(
                        "select count(*) from refound.user_blocks where blocker_id = ? and blocked_id = ?",
                        Integer.class, a.id(), b.id()))
                .isEqualTo(1);
    }

    @Test
    void duplicateBlockRejected() {
        TestUser a = users.a();
        TestUser b = users.b();
        block(a, b);
        assertThatThrownBy(() -> block(a, b)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void selfBlockRejected() {
        TestUser a = users.a();
        assertThatThrownBy(() -> block(a, a)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void termsVersionAndTimeGoTogether() {
        TestUser a = users.a();
        assertThat(jdbc.update(
                        "update refound.users set terms_version = 1, terms_accepted_at = now() where id = ?", a.id()))
                .isEqualTo(1);
    }

    @Test
    void termsVersionWithoutTimeRejected() {
        TestUser a = users.a();
        assertThatThrownBy(() -> jdbc.update("update refound.users set terms_version = 1 where id = ?", a.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void newUserHasNotAcceptedTerms() {
        TestUser a = users.a();
        assertThat(jdbc.queryForObject("select terms_version from refound.users where id = ?", Integer.class, a.id()))
                .isNull();
    }

    private void block(TestUser blocker, TestUser blocked) {
        jdbc.update("insert into refound.user_blocks (blocker_id, blocked_id) values (?, ?)", blocker.id(), blocked.id());
    }
}
