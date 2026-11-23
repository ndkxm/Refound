package com.refound.claim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.refound.common.InvalidTransitionException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

/** Mọi dòng bảng 05 §5.2, và mọi tổ hợp (trạng thái, hành động) ngoài bảng đều bị từ chối. */
class ClaimStateMachineTest {

    /** Đáp án chép tay từ bảng 5.2 (C1 là tạo mới, không qua {@code next}). */
    static final String[] TABLE = {
        "C2,  SUBMITTED, ACCEPT,          ACCEPTED",
        "C3,  SUBMITTED, REJECT,          REJECTED",
        "C4,  SUBMITTED, WITHDRAW,        CANCELLED",
        "C4b, SUBMITTED, AUTO_CANCEL,     CANCELLED",
        "C4b, SUBMITTED, AUTO_REJECT,     REJECTED",
        "C5,  ACCEPTED,  CANCEL,          CANCELLED",
        "C5b, ACCEPTED,  GENERATE_CODE,   ACCEPTED",
        "C6,  ACCEPTED,  SCAN_QR,         COMPLETED",
        "C6b, ACCEPTED,  MANUAL_COMPLETE, COMPLETED",
        "C7,  ACCEPTED,  DISPUTE,         DISPUTED",
        "C8,  DISPUTED,  RESOLVE,         CANCELLED",
        "C9,  SUBMITTED, EXPIRE,          CANCELLED",
        "C9,  ACCEPTED,  EXPIRE,          CANCELLED",
        "C10, SUBMITTED, BLOCK,           CANCELLED",
        "C10, ACCEPTED,  BLOCK,           CANCELLED",
    };

    @ParameterizedTest(name = "{0}")
    @FieldSource("TABLE")
    void tableRow(String line) {
        String[] c = line.split(",\\s*");
        assertThat(ClaimStateMachine.next(ClaimStatus.valueOf(c[1]), ClaimAction.valueOf(c[2])))
                .isEqualTo(ClaimStatus.valueOf(c[3]));
    }

    @Test
    void everythingOutsideTheTableIsRejected() {
        Map<String, ClaimStatus> allowed = new HashMap<>();
        for (String line : TABLE) {
            String[] c = line.split(",\\s*");
            allowed.put(c[1] + "/" + c[2], ClaimStatus.valueOf(c[3]));
        }
        int rejected = 0;
        for (ClaimStatus from : ClaimStatus.values()) {
            for (ClaimAction action : ClaimAction.values()) {
                ClaimStatus expected = allowed.get(from + "/" + action);
                if (expected != null) {
                    assertThat(ClaimStateMachine.next(from, action)).isEqualTo(expected);
                } else {
                    assertThatThrownBy(() -> ClaimStateMachine.next(from, action))
                            .as("%s --%s--> phải bị từ chối", from, action)
                            .isInstanceOf(InvalidTransitionException.class);
                    rejected++;
                }
            }
        }
        assertThat(rejected).isEqualTo(6 * 13 - 15);
    }

    @Test
    void finalStatesAcceptNothing() {
        for (ClaimStatus end : new ClaimStatus[] {ClaimStatus.COMPLETED, ClaimStatus.REJECTED, ClaimStatus.CANCELLED}) {
            for (ClaimAction action : ClaimAction.values()) {
                assertThatThrownBy(() -> ClaimStateMachine.next(end, action))
                        .isInstanceOf(InvalidTransitionException.class);
            }
        }
    }

    @Test
    void notableInvalidTransitions() {
        // quét QR / tạo mã trước khi Finder chấp nhận
        assertThatThrownBy(() -> ClaimStateMachine.next(ClaimStatus.SUBMITTED, ClaimAction.SCAN_QR))
                .isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> ClaimStateMachine.next(ClaimStatus.SUBMITTED, ClaimAction.GENERATE_CODE))
                .isInstanceOf(InvalidTransitionException.class);
        // chấp nhận hai lần (luồng thứ hai thua ở UPDATE ... WHERE status, đây là lớp hàm thuần)
        assertThatThrownBy(() -> ClaimStateMachine.next(ClaimStatus.ACCEPTED, ClaimAction.ACCEPT))
                .isInstanceOf(InvalidTransitionException.class);
        // rút sau khi đã chấp nhận phải đi đường hủy (C5) để tin về open
        assertThatThrownBy(() -> ClaimStateMachine.next(ClaimStatus.ACCEPTED, ClaimAction.WITHDRAW))
                .isInstanceOf(InvalidTransitionException.class);
        // tranh chấp chỉ admin xử lý, hai bên không hủy được
        assertThatThrownBy(() -> ClaimStateMachine.next(ClaimStatus.DISPUTED, ClaimAction.CANCEL))
                .isInstanceOf(InvalidTransitionException.class);
    }
}
