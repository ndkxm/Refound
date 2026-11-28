package com.refound.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.refound.support.TestUsers.TestUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** P1-01: migration V202611281000__core_p1 (02 §2.2). */
@IntegrationTest
@Transactional // Postgres hủy cả transaction sau câu lỗi đầu tiên: mỗi ca lỗi một test
class CoreP1MigrationIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestUsers users;

    @Autowired
    TestItems items;

    @Test
    void secondOpenClaimBySameClaimantRejected() {
        TestUser finder = users.a();
        TestUser owner = users.b();
        UUID item = items.found(finder).insert();
        claim(item, owner, finder, "submitted");
        assertThatThrownBy(() -> claim(item, owner, finder, "submitted"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void claimAgainAfterRejectedAllowed() {
        TestUser finder = users.a();
        TestUser owner = users.b();
        UUID item = items.found(finder).insert();
        UUID first = claim(item, owner, finder, "submitted");
        jdbc.update("update refound.claims set status = 'rejected', reject_reason = 'Sai màu' where id = ?", first);
        claim(item, owner, finder, "submitted");
        assertThat(jdbc.queryForObject(
                        "select count(*) from refound.claims where found_item_id = ?", Integer.class, item))
                .isEqualTo(2);
    }

    @Test
    void twoAcceptedClaimsOnOneItemRejected() {
        TestUser finder = users.a();
        UUID item = items.found(finder).insert();
        claim(item, users.b(), finder, "accepted");
        assertThatThrownBy(() -> claim(item, users.c(), finder, "accepted"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void otherClaimantsMaySubmitWhileOneIsAccepted() {
        TestUser finder = users.a();
        UUID item = items.found(finder).insert();
        claim(item, users.b(), finder, "accepted");
        claim(item, users.c(), finder, "submitted");
    }

    @Test
    void claimOwnItemRejected() {
        TestUser finder = users.a();
        UUID item = items.found(finder).insert();
        assertThatThrownBy(() -> claim(item, finder, finder, "submitted"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectedWithoutReasonRejected() {
        TestUser finder = users.a();
        UUID item = items.found(finder).insert();
        assertThatThrownBy(() -> claim(item, users.b(), finder, "rejected"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void manualHandoverNeedsNote() {
        TestUser finder = users.a();
        UUID claim = claim(items.found(finder).insert(), users.b(), finder, "accepted");
        assertThatThrownBy(() -> jdbc.update(
                        "insert into refound.handovers (claim_id, method, completed_at) values (?, 'manual', now())",
                        claim))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void handoverCodeAndExpiryGoTogether() {
        TestUser finder = users.a();
        UUID claim = claim(items.found(finder).insert(), users.b(), finder, "accepted");
        assertThatThrownBy(() -> jdbc.update(
                        "insert into refound.handovers (claim_id, code_hash) values (?, repeat('a', 64))", claim))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reportNeedsExactlyOneTarget() {
        TestUser a = users.a();
        assertThatThrownBy(() -> jdbc.update(
                        "insert into refound.reports (reporter_id, reason) values (?, 'Lừa đảo')", a.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameUserReportsItemOnce() {
        UUID item = items.found(users.a()).insert();
        TestUser b = users.b();
        report(item, b);
        assertThatThrownBy(() -> report(item, b)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void matchCandidateUpsertsByPair() {
        UUID lost = items.lost(users.b()).insert();
        UUID found = items.found(users.a()).insert();
        String upsert = """
                insert into refound.match_candidates (lost_item_id, found_item_id, score, components)
                values (?, ?, ?, '{"v":0.8,"d":1,"t":1,"c":1}')
                on conflict (lost_item_id, found_item_id) do update set score = excluded.score
                """;
        jdbc.update(upsert, lost, found, 0.5);
        jdbc.update(upsert, lost, found, 0.7);
        assertThat(jdbc.queryForObject(
                        "select score from refound.match_candidates where lost_item_id = ?", Double.class, lost))
                .isCloseTo(0.7, org.assertj.core.data.Offset.offset(1e-6));
    }

    private UUID claim(UUID item, TestUser claimant, TestUser finder, String status) {
        return jdbc.queryForObject(
                """
                insert into refound.claims (found_item_id, claimant_id, finder_id, status, answer)
                values (?, ?, ?, ?, 'Có móc khóa hình mèo') returning id
                """,
                UUID.class, item, claimant.id(), finder.id(), status);
    }

    private void report(UUID item, TestUser reporter) {
        jdbc.update(
                "insert into refound.reports (item_id, reporter_id, reason) values (?, ?, 'Tin giả')",
                item, reporter.id());
    }
}
