package com.refound.item.domain;

import com.refound.common.ApiException;
import com.refound.common.Viewer;
import java.util.UUID;

/**
 * Ai được làm gì với một tin: các dòng Item / ItemImage / VerificationSecret của ma trận 03 §3.2, hàm thuần.
 * Quy ước lỗi 03 §3.3: tin người gọi không được biết là có (under_review, đã xóa, trạng thái kín) → 404;
 * thấy được nhưng không có quyền sửa → 403.
 * Claimant được chấp nhận xem vị trí chính xác và ảnh nhạy cảm qua {@code ClaimDto} (06 §6.2), không qua đây.
 */
public final class ItemAccess {

    private ItemAccess() {}

    /** Những gì quyền phụ thuộc vào, lấy từ một dòng {@code items}. */
    public record Facts(UUID authorId, ItemType type, ItemStatus status, boolean deleted) {}

    /** Mức xem: không thấy, bản công khai ({@code ItemPublicDto}), bản đầy đủ ({@code ItemOwnerDto}). */
    public enum View {
        NONE,
        PUBLIC,
        FULL
    }

    public static View view(Facts item, Viewer me) {
        if (me.admin()) {
            return View.FULL; // admin xem mọi tin, kể cả đã xóa (03 §3.2)
        }
        if (item.deleted()) {
            return View.NONE; // I13: ẩn mọi nơi, kể cả với tác giả
        }
        if (me.is(item.authorId())) {
            return View.FULL;
        }
        return item.status() == ItemStatus.OPEN || item.status() == ItemStatus.CLAIMED ? View.PUBLIC : View.NONE;
    }

    /** @return mức xem, không bao giờ {@code NONE} */
    public static View requireView(Facts item, Viewer me) {
        View v = view(item, me);
        if (v == View.NONE) {
            throw notFound();
        }
        return v;
    }

    /** Sửa, đóng, gia hạn, báo đã tìm thấy, xóa: chỉ tác giả. Admin chỉ có restore/remove (P1-10). */
    public static void requireAuthor(Facts item, Viewer me) {
        requireView(item, me);
        if (!me.is(item.authorId())) {
            throw ApiException.forbidden("Chỉ người đăng tin mới thực hiện được thao tác này.");
        }
    }

    /** BR-02: đáp án câu hỏi kín chỉ tác giả tin {@code found} đọc được; mọi người khác, kể cả admin, nhận 404 (X6). */
    public static void requireSecretReader(Facts item, Viewer me) {
        if (item.deleted() || item.type() != ItemType.FOUND || !me.is(item.authorId())) {
            throw notFound();
        }
    }

    /** BR-08, FR-ITEM-07: bản công khai của tin nhạy cảm không có ảnh; bản đầy đủ luôn có. */
    public static boolean imagesVisible(View view, boolean sensitive) {
        return view == View.FULL || (view == View.PUBLIC && !sensitive);
    }

    private static ApiException notFound() {
        return ApiException.notFound("Không tìm thấy tin.");
    }
}
