package com.refound.claim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.refound.claim.domain.ClaimAccess.Parties;
import com.refound.common.ApiException;
import com.refound.common.Viewer;
import com.refound.item.domain.ItemAccess.Facts;
import com.refound.item.domain.ItemStatus;
import com.refound.item.domain.ItemType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Dòng Claim / ChatMessage của ma trận 03 §3.2 (tên khác {@code *AccessTest} để chạy ở pha test). */
class ClaimAccessRulesTest {

    static final Viewer OWNER = new Viewer(UUID.randomUUID(), false);
    static final Viewer FINDER = new Viewer(UUID.randomUUID(), false);
    static final Viewer C = new Viewer(UUID.randomUUID(), false);
    static final Viewer ADMIN = new Viewer(UUID.randomUUID(), true);
    static final Parties CLAIM = new Parties(OWNER.id(), FINDER.id());

    @Test
    void viewIsBothPartiesAndAdmin() {
        ok(() -> ClaimAccess.requireView(CLAIM, OWNER));
        ok(() -> ClaimAccess.requireView(CLAIM, FINDER));
        ok(() -> ClaimAccess.requireView(CLAIM, ADMIN));
        fails(() -> ClaimAccess.requireView(CLAIM, C), 404); // X11
    }

    /** X14: chat chỉ hai bên; admin xem claim được nhưng không đọc chat (03 §3.2). */
    @Test
    void participantExcludesAdminAndStrangers() {
        ok(() -> ClaimAccess.requireParticipant(CLAIM, OWNER));
        ok(() -> ClaimAccess.requireParticipant(CLAIM, FINDER));
        fails(() -> ClaimAccess.requireParticipant(CLAIM, ADMIN), 404);
        fails(() -> ClaimAccess.requireParticipant(CLAIM, C), 404);
    }

    /** X12: claimant gọi hành động của Finder → 403; người ngoài → 404. */
    @Test
    void finderOnly() {
        ok(() -> ClaimAccess.requireFinder(CLAIM, FINDER));
        fails(() -> ClaimAccess.requireFinder(CLAIM, OWNER), 403);
        fails(() -> ClaimAccess.requireFinder(CLAIM, C), 404);
        fails(() -> ClaimAccess.requireFinder(CLAIM, ADMIN), 404);
    }

    /** X13: Finder tự quét → 403; C quét mã đúng → 404. */
    @Test
    void claimantOnly() {
        ok(() -> ClaimAccess.requireClaimant(CLAIM, OWNER));
        fails(() -> ClaimAccess.requireClaimant(CLAIM, FINDER), 403);
        fails(() -> ClaimAccess.requireClaimant(CLAIM, C), 404);
    }

    @Test
    void submitOnOthersOpenFoundItem() {
        Facts found = new Facts(FINDER.id(), ItemType.FOUND, ItemStatus.OPEN, false);
        ok(() -> ClaimAccess.requireCanSubmit(found, null, OWNER));
        ok(() -> ClaimAccess.requireCanSubmit(found, OWNER.id(), OWNER)); // gắn tin lost của chính mình
    }

    @Test
    void submitRejections() {
        Facts found = new Facts(FINDER.id(), ItemType.FOUND, ItemStatus.OPEN, false);
        fails(() -> ClaimAccess.requireCanSubmit(found, null, FINDER), 403); // X9
        fails(() -> ClaimAccess.requireCanSubmit(found, C.id(), OWNER), 403); // X10: tin lost của C
        fails(() -> ClaimAccess.requireCanSubmit(found, null, ADMIN), 403);

        Facts claimed = new Facts(FINDER.id(), ItemType.FOUND, ItemStatus.CLAIMED, false);
        fails(() -> ClaimAccess.requireCanSubmit(claimed, null, OWNER), 409);
        Facts lost = new Facts(FINDER.id(), ItemType.LOST, ItemStatus.OPEN, false);
        fails(() -> ClaimAccess.requireCanSubmit(lost, null, OWNER), 409);
        // tin không được thấy thì như không tồn tại
        Facts underReview = new Facts(FINDER.id(), ItemType.FOUND, ItemStatus.UNDER_REVIEW, false);
        fails(() -> ClaimAccess.requireCanSubmit(underReview, null, OWNER), 404);
        Facts deleted = new Facts(FINDER.id(), ItemType.FOUND, ItemStatus.OPEN, true);
        fails(() -> ClaimAccess.requireCanSubmit(deleted, null, OWNER), 404);
    }

    /** X15: gửi chỉ khi accepted; đọc lại được sau khi kết thúc. */
    @ParameterizedTest(name = "{0}: gửi {1}, đọc {2}")
    @CsvSource({
        "SUBMITTED, false, false",
        "ACCEPTED,  true,  true",
        "REJECTED,  false, false",
        "CANCELLED, false, true",
        "COMPLETED, false, true",
        "DISPUTED,  false, true",
    })
    void chatRules(ClaimStatus status, boolean write, boolean read) {
        assertThat(ClaimAccess.canWriteChat(status)).isEqualTo(write);
        assertThat(ClaimAccess.canReadChat(status)).isEqualTo(read);
    }

    private static void ok(Executable call) {
        assertThatCode(call::execute).doesNotThrowAnyException();
    }

    private static void fails(Executable call, int status) {
        assertThatThrownBy(call::execute).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.status().value()).isEqualTo(status));
    }
}
