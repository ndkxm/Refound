package com.refound.claim.domain;

import com.refound.common.ApiException;
import com.refound.common.Viewer;
import com.refound.item.domain.ItemAccess;
import com.refound.item.domain.ItemStatus;
import com.refound.item.domain.ItemType;
import java.util.UUID;

/**
 * Ai được làm gì với một claim: các dòng Claim / ChatMessage / Handover của ma trận 03 §3.2, hàm thuần.
 * Người không phải hai bên không được biết claim tồn tại → 404 (X11, X14); là một bên nhưng sai vai → 403 (X12, X13).
 */
public final class ClaimAccess {

    private ClaimAccess() {}

    /** Hai bên của claim. {@code finderId} sao chép từ tác giả tin {@code found} lúc tạo claim (02 §2.2). */
    public record Parties(UUID claimantId, UUID finderId) {

        boolean includes(Viewer me) {
            return me.is(claimantId) || me.is(finderId);
        }
    }

    /** Xem claim: hai bên; admin xem siêu dữ liệu và lý do (không chat, xem {@link #requireParticipant}). */
    public static void requireView(Parties claim, Viewer me) {
        if (!me.admin() && !claim.includes(me)) {
            throw notFound();
        }
    }

    /** Chat và các hành động chung (hủy, tranh chấp): chỉ hai bên, kể cả admin cũng 404. */
    public static void requireParticipant(Parties claim, Viewer me) {
        if (!claim.includes(me)) {
            throw notFound();
        }
    }

    /** Chấp nhận, từ chối, tạo mã QR, tự xác nhận: chỉ Finder (X12). */
    public static void requireFinder(Parties claim, Viewer me) {
        requireParticipant(claim, me);
        if (!me.is(claim.finderId())) {
            throw ApiException.forbidden("Chỉ người nhặt được mới thực hiện được thao tác này.");
        }
    }

    /** Rút, quét QR: chỉ Owner (X13: Finder tự quét → 403). */
    public static void requireClaimant(Parties claim, Viewer me) {
        requireParticipant(claim, me);
        if (!me.is(claim.claimantId())) {
            throw ApiException.forbidden("Chỉ người gửi yêu cầu mới thực hiện được thao tác này.");
        }
    }

    /**
     * C1, phần quyền và trạng thái tin. Phần cần DB (claim mở trùng, BR-05, chặn) nằm ở service.
     *
     * @param lostItemAuthorId tác giả của {@code lostItemId} gửi kèm, hoặc null nếu không gửi
     */
    public static void requireCanSubmit(ItemAccess.Facts foundItem, UUID lostItemAuthorId, Viewer me) {
        ItemAccess.requireView(foundItem, me); // tin under_review, đã xóa → 404 như không tồn tại
        if (me.is(foundItem.authorId())) {
            throw ApiException.forbidden("Bạn không thể nhận tin do chính mình đăng."); // X9
        }
        if (me.admin()) {
            throw ApiException.forbidden("Tài khoản quản trị không gửi yêu cầu nhận đồ."); // 03 §3.2
        }
        if (lostItemAuthorId != null && !me.is(lostItemAuthorId)) {
            throw ApiException.forbidden("Tin mất gắn kèm phải là tin của bạn."); // X10
        }
        if (foundItem.type() != ItemType.FOUND || foundItem.status() != ItemStatus.OPEN) {
            throw ApiException.invalidState("Tin này hiện không nhận yêu cầu.");
        }
    }

    /** Gửi tin nhắn chỉ khi claim {@code accepted} (X15: {@code submitted} hoặc {@code completed} → 409). */
    public static boolean canWriteChat(ClaimStatus status) {
        return status == ClaimStatus.ACCEPTED;
    }

    /** Đọc lại tin nhắn được cả sau khi claim kết thúc; claim chưa từng được chấp nhận thì không có chat. */
    public static boolean canReadChat(ClaimStatus status) {
        return status != ClaimStatus.SUBMITTED && status != ClaimStatus.REJECTED;
    }

    private static ApiException notFound() {
        return ApiException.notFound("Không tìm thấy yêu cầu nhận.");
    }
}
