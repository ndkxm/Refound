package com.refound.item.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.refound.common.ApiException;
import com.refound.common.Viewer;
import com.refound.item.domain.ItemAccess.Facts;
import com.refound.item.domain.ItemAccess.View;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** Dòng Item / ItemImage / VerificationSecret của ma trận 03 §3.2 (tên khác {@code *AccessTest} để chạy ở pha test). */
class ItemAccessRulesTest {

    static final UUID AUTHOR = UUID.randomUUID();
    static final Viewer A = new Viewer(AUTHOR, false);
    static final Viewer B = new Viewer(UUID.randomUUID(), false);
    static final Viewer ADMIN = new Viewer(UUID.randomUUID(), true);

    @ParameterizedTest(name = "{0}, đã xóa={1}: tác giả {2}, người lạ {3}, admin {4}")
    @CsvSource({
        "OPEN,         false, FULL, PUBLIC, FULL",
        "CLAIMED,      false, FULL, PUBLIC, FULL",
        "RETURNED,     false, FULL, NONE,   FULL",
        "CLOSED,       false, FULL, NONE,   FULL",
        "EXPIRED,      false, FULL, NONE,   FULL",
        "UNDER_REVIEW, false, FULL, NONE,   FULL", // X4: người lạ 404
        "OPEN,         true,  NONE, NONE,   FULL", // X4, I13: xóa mềm ẩn cả với tác giả
        "CLOSED,       true,  NONE, NONE,   FULL",
    })
    void viewMatrix(ItemStatus status, boolean deleted, View author, View stranger, View admin) {
        for (ItemType type : ItemType.values()) {
            Facts item = new Facts(AUTHOR, type, status, deleted);
            assertThat(ItemAccess.view(item, A)).isEqualTo(author);
            assertThat(ItemAccess.view(item, B)).isEqualTo(stranger);
            assertThat(ItemAccess.view(item, ADMIN)).isEqualTo(admin);
        }
    }

    @Test
    void requireViewThrows404WhenHidden() {
        Facts underReview = new Facts(AUTHOR, ItemType.FOUND, ItemStatus.UNDER_REVIEW, false);
        assertThatThrownBy(() -> ItemAccess.requireView(underReview, B)).satisfies(e -> status(e, 404, "NOT_FOUND"));
        assertThat(ItemAccess.requireView(underReview, A)).isEqualTo(View.FULL);
    }

    /** X1, X2: B sửa/đóng/xóa tin A đang công khai → 403; tin B không được thấy → 404; admin → 403. */
    @Test
    void onlyAuthorModifies() {
        Facts open = new Facts(AUTHOR, ItemType.LOST, ItemStatus.OPEN, false);
        assertThatCode(() -> ItemAccess.requireAuthor(open, A)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ItemAccess.requireAuthor(open, B)).satisfies(e -> status(e, 403, "FORBIDDEN"));
        assertThatThrownBy(() -> ItemAccess.requireAuthor(open, ADMIN)).satisfies(e -> status(e, 403, "FORBIDDEN"));

        Facts expired = new Facts(AUTHOR, ItemType.LOST, ItemStatus.EXPIRED, false);
        assertThatThrownBy(() -> ItemAccess.requireAuthor(expired, B)).satisfies(e -> status(e, 404, "NOT_FOUND"));

        Facts deleted = new Facts(AUTHOR, ItemType.LOST, ItemStatus.OPEN, true);
        assertThatThrownBy(() -> ItemAccess.requireAuthor(deleted, A)).satisfies(e -> status(e, 404, "NOT_FOUND"));
    }

    /** X6, BR-02: chỉ tác giả tin found đọc đáp án; B, admin, tin lost, tin đã xóa → 404. */
    @Test
    void secretOnlyForFoundAuthor() {
        Facts found = new Facts(AUTHOR, ItemType.FOUND, ItemStatus.OPEN, false);
        assertThatCode(() -> ItemAccess.requireSecretReader(found, A)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ItemAccess.requireSecretReader(found, B)).satisfies(e -> status(e, 404, "NOT_FOUND"));
        assertThatThrownBy(() -> ItemAccess.requireSecretReader(found, ADMIN))
                .satisfies(e -> status(e, 404, "NOT_FOUND"));

        Facts lost = new Facts(AUTHOR, ItemType.LOST, ItemStatus.OPEN, false);
        assertThatThrownBy(() -> ItemAccess.requireSecretReader(lost, A)).satisfies(e -> status(e, 404, "NOT_FOUND"));

        Facts deleted = new Facts(AUTHOR, ItemType.FOUND, ItemStatus.CLOSED, true);
        assertThatThrownBy(() -> ItemAccess.requireSecretReader(deleted, A))
                .satisfies(e -> status(e, 404, "NOT_FOUND"));
    }

    /** X5, BR-08: tin nhạy cảm không có ảnh ở bản công khai. */
    @ParameterizedTest
    @EnumSource(View.class)
    void imagesVisibility(View view) {
        assertThat(ItemAccess.imagesVisible(view, false)).isEqualTo(view != View.NONE);
        assertThat(ItemAccess.imagesVisible(view, true)).isEqualTo(view == View.FULL);
    }

    static void status(Throwable e, int status, String code) {
        assertThat(e).isInstanceOf(ApiException.class);
        assertThat(((ApiException) e).status().value()).isEqualTo(status);
        assertThat(((ApiException) e).code()).isEqualTo(code);
    }
}
