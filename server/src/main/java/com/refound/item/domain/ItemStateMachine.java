package com.refound.item.domain;

import static com.refound.item.domain.ItemStatus.CLAIMED;
import static com.refound.item.domain.ItemStatus.CLOSED;
import static com.refound.item.domain.ItemStatus.EXPIRED;
import static com.refound.item.domain.ItemStatus.OPEN;
import static com.refound.item.domain.ItemStatus.RETURNED;
import static com.refound.item.domain.ItemStatus.UNDER_REVIEW;

import com.refound.common.InvalidTransitionException;
import java.util.EnumSet;
import java.util.Set;

/**
 * Bảng chuyển trạng thái tin (05 §5.1), hàm thuần. Chỉ quyết chuyển nào hợp lệ; ai được làm nằm ở
 * {@link ItemAccess}, ghi DB nằm ở service với {@code UPDATE ... WHERE status = :old}.
 * Điều kiện "không có claim accepted" của I2, I7, I13 đã nằm trong trạng thái: có claim accepted thì tin là
 * {@code claimed} (I3), mà {@code claimed} không có trong các dòng đó.
 */
public final class ItemStateMachine {

    /** I13: xóa được khi không có bàn giao đang dở ({@code claimed}) và không đang chờ admin ({@code under_review}). */
    static final Set<ItemStatus> DELETABLE = EnumSet.of(OPEN, CLOSED, EXPIRED, RETURNED);

    private ItemStateMachine() {}

    /** @throws InvalidTransitionException chuyển không có trong bảng (API trả 409 {@code INVALID_STATE}) */
    public static ItemStatus next(ItemStatus from, ItemAction action, ItemType type) {
        ItemStatus to = switch (action) {
            case EDIT, WARN_EXPIRY -> from == OPEN ? OPEN : null;
            case CLAIM_ACCEPTED -> from == OPEN && type == ItemType.FOUND ? CLAIMED : null;
            case CLAIM_RELEASED -> from == CLAIMED ? OPEN : null;
            // tin found: claimed → returned; tin lost liên kết với claim vừa xong: open → returned (I5)
            case HANDOVER_DONE -> from == CLAIMED || (from == OPEN && type == ItemType.LOST) ? RETURNED : null;
            case MARK_FOUND -> from == OPEN && type == ItemType.LOST ? RETURNED : null;
            case CLOSE -> from == OPEN ? CLOSED : null;
            case EXPIRE -> from == OPEN ? EXPIRED : null;
            case RENEW -> from == EXPIRED ? OPEN : null;
            // tin claimed bị báo cáo vẫn giữ nguyên để không cắt ngang bàn giao (05 §5.1)
            case REPORT_THRESHOLD -> from == OPEN ? UNDER_REVIEW : null;
            case RESTORE -> from == UNDER_REVIEW ? OPEN : null;
            case REMOVE -> from == UNDER_REVIEW ? CLOSED : null;
            case DELETE -> DELETABLE.contains(from) ? from : null;
        };
        if (to == null) {
            throw new InvalidTransitionException(from, action);
        }
        return to;
    }
}
