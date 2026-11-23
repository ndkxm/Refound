package com.refound.claim.domain;

import static com.refound.claim.domain.ClaimStatus.ACCEPTED;
import static com.refound.claim.domain.ClaimStatus.CANCELLED;
import static com.refound.claim.domain.ClaimStatus.COMPLETED;
import static com.refound.claim.domain.ClaimStatus.DISPUTED;
import static com.refound.claim.domain.ClaimStatus.REJECTED;
import static com.refound.claim.domain.ClaimStatus.SUBMITTED;

import com.refound.common.InvalidTransitionException;

/**
 * Bảng chuyển trạng thái claim (05 §5.2), hàm thuần. Claim mới luôn {@link ClaimStatus#SUBMITTED} (C1); điều kiện
 * tạo nằm ở {@link ClaimAccess#requireCanSubmit} và service. {@code completed}, {@code rejected}, {@code cancelled}
 * là trạng thái cuối.
 */
public final class ClaimStateMachine {

    private ClaimStateMachine() {}

    /** @throws InvalidTransitionException chuyển không có trong bảng (API trả 409 {@code INVALID_STATE}) */
    public static ClaimStatus next(ClaimStatus from, ClaimAction action) {
        ClaimStatus to = switch (from) {
            case SUBMITTED -> switch (action) {
                case ACCEPT -> ACCEPTED;
                case REJECT, AUTO_REJECT -> REJECTED;
                case WITHDRAW, AUTO_CANCEL, EXPIRE, BLOCK -> CANCELLED;
                default -> null;
            };
            case ACCEPTED -> switch (action) {
                case GENERATE_CODE -> ACCEPTED;
                case SCAN_QR, MANUAL_COMPLETE -> COMPLETED;
                case CANCEL, EXPIRE, BLOCK -> CANCELLED;
                case DISPUTE -> DISPUTED;
                default -> null;
            };
            case DISPUTED -> action == ClaimAction.RESOLVE ? CANCELLED : null;
            case REJECTED, CANCELLED, COMPLETED -> null;
        };
        if (to == null) {
            throw new InvalidTransitionException(from, action);
        }
        return to;
    }
}
