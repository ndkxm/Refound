package com.refound.item.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.refound.common.InvalidTransitionException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

/** Mọi dòng bảng 05 §5.1, và mọi tổ hợp (trạng thái, hành động, loại) ngoài bảng đều bị từ chối. */
class ItemStateMachineTest {

    /** Đáp án chép tay từ bảng 5.1. Loại {@code *} = cả lost và found. */
    static final String[] TABLE = {
        "I2,   OPEN,         EDIT,             *,     OPEN",
        "I3,   OPEN,         CLAIM_ACCEPTED,   FOUND, CLAIMED",
        "I4,   CLAIMED,      CLAIM_RELEASED,   *,     OPEN",
        "I5,   CLAIMED,      HANDOVER_DONE,    *,     RETURNED",
        "I5,   OPEN,         HANDOVER_DONE,    LOST,  RETURNED",
        "I6,   OPEN,         MARK_FOUND,       LOST,  RETURNED",
        "I7,   OPEN,         CLOSE,            *,     CLOSED",
        "I8,   OPEN,         EXPIRE,           *,     EXPIRED",
        "I8a,  OPEN,         WARN_EXPIRY,      *,     OPEN",
        "I9,   EXPIRED,      RENEW,            *,     OPEN",
        "I10,  OPEN,         REPORT_THRESHOLD, *,     UNDER_REVIEW",
        "I11,  UNDER_REVIEW, RESTORE,          *,     OPEN",
        "I12,  UNDER_REVIEW, REMOVE,           *,     CLOSED",
        "I13,  OPEN,         DELETE,           *,     OPEN",
        "I13,  CLOSED,       DELETE,           *,     CLOSED",
        "I13,  EXPIRED,      DELETE,           *,     EXPIRED",
        "I13,  RETURNED,     DELETE,           *,     RETURNED",
    };

    @ParameterizedTest(name = "{0}")
    @FieldSource("TABLE")
    void tableRow(String line) {
        String[] c = line.split(",\s*");
        for (ItemType t : types(c[3].strip())) {
            assertThat(ItemStateMachine.next(ItemStatus.valueOf(c[1].strip()), ItemAction.valueOf(c[2].strip()), t))
                    .isEqualTo(ItemStatus.valueOf(c[4].strip()));
        }
    }

    @Test
    void everythingOutsideTheTableIsRejected() {
        Map<String, ItemStatus> allowed = new HashMap<>();
        for (String line : TABLE) {
            String[] c = line.split(",\\s*");
            for (ItemType t : types(c[3].strip())) {
                allowed.put(key(ItemStatus.valueOf(c[1].strip()), ItemAction.valueOf(c[2].strip()), t),
                        ItemStatus.valueOf(c[4].strip()));
            }
        }
        int rejected = 0;
        for (ItemStatus from : ItemStatus.values()) {
            for (ItemAction action : ItemAction.values()) {
                for (ItemType type : ItemType.values()) {
                    ItemStatus expected = allowed.get(key(from, action, type));
                    if (expected != null) {
                        assertThat(ItemStateMachine.next(from, action, type)).isEqualTo(expected);
                    } else {
                        assertThatThrownBy(() -> ItemStateMachine.next(from, action, type))
                                .as("%s --%s/%s--> phải bị từ chối", from, action, type)
                                .isInstanceOf(InvalidTransitionException.class);
                        rejected++;
                    }
                }
            }
        }
        // 6 trạng thái × 13 hành động × 2 loại = 156 tổ hợp; 31 hợp lệ khi tách {@code *} thành hai loại
        assertThat(allowed).hasSize(31);
        assertThat(rejected).isEqualTo(156 - 31);
    }

    @Test
    void notableInvalidTransitions() {
        // claim chỉ trên tin found (I3), báo đã tìm thấy chỉ tin lost (I6)
        assertRejected(ItemStatus.OPEN, ItemAction.CLAIM_ACCEPTED, ItemType.LOST);
        assertRejected(ItemStatus.OPEN, ItemAction.MARK_FOUND, ItemType.FOUND);
        // không sửa, đóng, xóa khi đang bàn giao (có claim accepted)
        assertRejected(ItemStatus.CLAIMED, ItemAction.EDIT, ItemType.FOUND);
        assertRejected(ItemStatus.CLAIMED, ItemAction.CLOSE, ItemType.FOUND);
        assertRejected(ItemStatus.CLAIMED, ItemAction.DELETE, ItemType.FOUND);
        // tin claimed bị báo cáo không tự chuyển under_review
        assertRejected(ItemStatus.CLAIMED, ItemAction.REPORT_THRESHOLD, ItemType.FOUND);
        // trạng thái cuối và chờ admin
        assertRejected(ItemStatus.RETURNED, ItemAction.RENEW, ItemType.LOST);
        assertRejected(ItemStatus.CLOSED, ItemAction.RENEW, ItemType.LOST);
        assertRejected(ItemStatus.UNDER_REVIEW, ItemAction.DELETE, ItemType.FOUND);
        assertRejected(ItemStatus.OPEN, ItemAction.RENEW, ItemType.LOST);
    }

    @Test
    void rejectionCarriesCodeAndStatus() {
        assertThatThrownBy(() -> ItemStateMachine.next(ItemStatus.CLOSED, ItemAction.EDIT, ItemType.LOST))
                .isInstanceOfSatisfying(InvalidTransitionException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.code()).isEqualTo("INVALID_STATE");
                    assertThat(e.properties()).containsEntry("from", "closed").containsEntry("action", "edit");
                });
    }

    private static void assertRejected(ItemStatus from, ItemAction action, ItemType type) {
        assertThatThrownBy(() -> ItemStateMachine.next(from, action, type))
                .isInstanceOf(InvalidTransitionException.class);
    }

    private static List<ItemType> types(String type) {
        return "*".equals(type) ? List.of(ItemType.values()) : List.of(ItemType.valueOf(type));
    }

    private static String key(ItemStatus s, ItemAction a, ItemType t) {
        return s + "/" + a + "/" + t;
    }
}
