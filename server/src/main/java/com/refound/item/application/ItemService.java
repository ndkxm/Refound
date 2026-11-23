package com.refound.item.application;

import com.refound.common.ApiException;
import com.refound.common.ApiException.FieldError;
import com.refound.common.GeoPoint;
import com.refound.common.Jpeg;
import com.refound.common.Viewer;
import com.refound.common.storage.StorageClient;
import com.refound.item.application.ItemDtos.ItemOwnerDto;
import com.refound.item.domain.Category;
import com.refound.item.domain.Item;
import com.refound.item.domain.ItemAccess;
import com.refound.item.domain.ItemAction;
import com.refound.item.domain.ItemImage;
import com.refound.item.domain.ItemRules;
import com.refound.item.domain.ItemRules.Draft;
import com.refound.item.domain.ItemStateMachine;
import com.refound.item.domain.ItemStatus;
import com.refound.item.domain.ItemType;
import com.refound.item.domain.LocationRounding;
import com.refound.item.domain.PostingLimit;
import com.refound.item.infrastructure.CategoryRepository;
import com.refound.item.infrastructure.ItemRepository;
import com.refound.item.infrastructure.ItemRepository.Secret;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

/** Ghi tin: tạo (I1), sửa (I2), đóng (I7), báo đã tìm thấy (I6), gia hạn (I9), xóa (I13). FR-ITEM-01, 02, 05. */
@Service
public class ItemService {

    static final int MAX_IMAGE_BYTES = 1024 * 1024; // ADR-06
    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository items;
    private final CategoryRepository categories;
    private final StorageClient storage;
    private final ItemViews views;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ItemService(ItemRepository items, CategoryRepository categories, StorageClient storage, ItemViews views,
            TransactionTemplate tx, ApplicationEventPublisher events, Clock clock) {
        this.items = items;
        this.categories = categories;
        this.storage = storage;
        this.views = views;
        this.tx = tx;
        this.events = events;
        this.clock = clock;
    }

    /** Ảnh của một vị trí: bản lớn 1024px và bản nhỏ 512px, đều JPEG do app resize (ADR-06). */
    public record ImageUpload(byte[] full, byte[] thumb) {}

    /**
     * Kiểm dữ liệu → kiểm BR-09 → upload ảnh → một transaction ghi tin, ảnh, câu hỏi kín. Ảnh lên Storage trước
     * transaction: Storage lỗi thì 503 và DB không có gì (7.3).
     */
    public ItemOwnerDto create(Viewer me, Draft draft, List<ImageUpload> images) {
        Instant now = clock.instant();
        Category category = categories.find(draft.category()).orElse(null);
        List<FieldError> errors = new ArrayList<>(ItemRules.validate(
                draft, category != null, category != null && category.sensitive(), images.size(), now));
        errors.addAll(imageErrors(images));
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
        requireUnderLimit(items.createdSince(me.id(), now.minus(PostingLimit.WINDOW)), now); // chặn sớm, trước upload

        UUID id = UUID.randomUUID();
        List<ItemImage> stored = upload(id, images);
        Item item = new Item(id, me.id(), draft.type(), ItemStatus.OPEN, category.code(), category.sensitive(),
                draft.description(), draft.happenedAt(), draft.location(),
                LocationRounding.publicLocation(draft.type(), draft.location()), draft.radiusM(), "pending", null,
                now, now, now, null);

        tx.executeWithoutResult(status -> {
            items.lockAuthor(me.id());
            // đếm lại trong khóa: request song song khác có thể vừa ghi xong. Ảnh đã upload thì thành rác.
            // shortcut: không xóa ảnh rác khi thua ở đây (rất hiếm), thêm StorageClient.delete nếu Storage đầy
            requireUnderLimit(items.createdSince(me.id(), now.minus(PostingLimit.WINDOW)), now);
            items.insert(item);
            stored.forEach(items::insertImage);
            if (draft.type() == ItemType.FOUND) {
                items.upsertSecret(id, draft.question(), draft.answer());
            }
            events.publishEvent(new ItemContentChanged(id)); // job embedding chạy sau commit (I1)
        });
        return views.owner(item, stored);
    }

    /** Phần {@code PATCH /items/{id}}: trường null là giữ nguyên; chuỗi đã bỏ khoảng trắng (rỗng thì bị báo lỗi độ dài). */
    public record Edit(
            String description,
            String category,
            Instant happenedAt,
            GeoPoint location,
            Integer radiusM,
            String question,
            String answer) {}

    /**
     * I2: tác giả sửa tin {@code open}. Kiểm lại toàn bộ tin sau khi gộp (đổi sang danh mục nhạy cảm mà tin mất có
     * ảnh → 400). Đổi mô tả hoặc danh mục thì vector cũ bị xóa và tin vào lại hàng đợi embedding; danh mục mới nhạy
     * cảm thì bỏ mô tả ảnh của AI (BR-08). Đổi vị trí tính lại vị trí công khai.
     */
    public ItemOwnerDto edit(Viewer me, UUID id, Edit edit) {
        Instant now = clock.instant();
        Item item = load(id);
        ItemAccess.requireAuthor(item.facts(), me);
        ItemStateMachine.next(item.status(), ItemAction.EDIT, item.type());

        String categoryCode = edit.category() != null ? edit.category() : item.category();
        Category category = categories.find(categoryCode).orElse(null);
        Secret secret = item.type() == ItemType.FOUND ? items.findSecret(id).orElse(null) : null;
        GeoPoint location = edit.location() != null ? edit.location() : item.location();
        Draft merged = new Draft(item.type(), categoryCode, or(edit.description(), item.description()),
                or(edit.happenedAt(), item.happenedAt()), location, or(edit.radiusM(), item.radiusM()),
                // tin mất: câu hỏi rỗng app gửi kèm coi như không gửi; có nội dung thì bị từ chối (BR-13)
                item.type() == ItemType.FOUND
                        ? or(edit.question(), secret == null ? null : secret.question()) : ItemRules.clean(edit.question()),
                item.type() == ItemType.FOUND
                        ? or(edit.answer(), secret == null ? null : secret.answer()) : ItemRules.clean(edit.answer()));
        int imageCount = items.findImages(List.of(id)).size();
        // thời điểm cũ không đổi thì vốn đã qua kiểm "không ở tương lai" lúc tạo
        List<FieldError> errors = ItemRules.validate(merged, category != null,
                category != null && category.sensitive(), imageCount, now);
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }

        Item next = new Item(id, item.authorId(), item.type(), item.status(), category.code(), category.sensitive(),
                merged.description(), merged.happenedAt(), location,
                LocationRounding.publicLocation(item.type(), location), merged.radiusM(), item.embeddingStatus(),
                item.closedReason(), item.createdAt(), now, now, null);
        boolean reembed = !next.description().equals(item.description()) || !next.category().equals(item.category());
        tx.executeWithoutResult(status -> {
            if (!items.update(next, reembed, category.sensitive(), now)) {
                throw changedMeanwhile();
            }
            if (reembed) {
                items.deleteEmbeddings(id); // I2: vector cũ không còn đúng với nội dung mới
                events.publishEvent(new ItemContentChanged(id));
            }
            if (item.type() == ItemType.FOUND
                    && (secret == null || !merged.question().equals(secret.question()) || !merged.answer().equals(secret.answer()))) {
                items.upsertSecret(id, merged.question(), merged.answer());
            }
        });
        return ownerView(id);
    }

    /** I7: đóng tin kèm lý do ngắn. Claim {@code submitted} → {@code rejected} thêm ở P1-05. */
    public ItemOwnerDto close(Viewer me, UUID id, String reason) {
        int n = reason == null ? 0 : reason.codePointCount(0, reason.length());
        if (n < 1 || n > 200) {
            throw ApiException.validation(List.of(new FieldError("reason", "Lý do dài 1–200 ký tự")));
        }
        return transition(me, id, ItemAction.CLOSE, reason, false);
    }

    /** I6: chủ tin {@code lost} tự báo đã tìm thấy. Đóng {@code match_candidates} thêm ở P1-02. */
    public ItemOwnerDto markFound(Viewer me, UUID id) {
        return transition(me, id, ItemAction.MARK_FOUND, null, false);
    }

    /** I9: gia hạn tin hết hạn; là hoạt động nên đặt lại mốc 30 ngày (BR-07). */
    public ItemOwnerDto renew(Viewer me, UUID id) {
        return transition(me, id, ItemAction.RENEW, null, true);
    }

    /** I13: xóa mềm. Tin đang bàn giao ({@code claimed}) hoặc chờ admin → 409. */
    public void delete(Viewer me, UUID id) {
        Item item = load(id);
        ItemAccess.requireAuthor(item.facts(), me);
        ItemStateMachine.next(item.status(), ItemAction.DELETE, item.type());
        if (!items.softDelete(id, item.status(), clock.instant())) {
            throw changedMeanwhile();
        }
    }

    private ItemOwnerDto transition(Viewer me, UUID id, ItemAction action, String closedReason, boolean touchActivity) {
        Item item = load(id);
        ItemAccess.requireAuthor(item.facts(), me);
        ItemStatus to = ItemStateMachine.next(item.status(), action, item.type());
        if (!items.changeStatus(id, item.status(), to, closedReason, touchActivity, clock.instant())) {
            throw changedMeanwhile();
        }
        return ownerView(id);
    }

    private Item load(UUID id) {
        return items.findById(id).orElseThrow(ItemQueryService::notFound);
    }

    private ItemOwnerDto ownerView(UUID id) {
        return views.owner(load(id), items.findImages(List.of(id)));
    }

    /** Trạng thái đổi giữa lúc đọc và lúc ghi ({@code UPDATE ... WHERE status} cập nhật 0 dòng, 05). */
    private static ApiException changedMeanwhile() {
        return ApiException.invalidState("Tin vừa được thay đổi, vui lòng tải lại.");
    }

    private static <T> T or(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static void requireUnderLimit(List<Instant> createdInWindow, Instant now) {
        PostingLimit.retryAfter(createdInWindow, now).ifPresent(wait -> {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Bạn đã đăng " + PostingLimit.MAX_PER_WINDOW + " tin trong 24 giờ qua, vui lòng thử lại sau.",
                    Map.of("retryAfterSeconds", ceilSeconds(wait)));
        });
    }

    private static long ceilSeconds(Duration d) {
        return d.toSeconds() + (d.toNanosPart() > 0 ? 1 : 0);
    }

    /**
     * JPEG thật (byte đầu), ≤ 1 MB. Tomcat đã chặn > 1 MB trước ({@code max-file-size}); đây là lớp thứ hai và trả
     * cùng mã 413 {@code PAYLOAD_TOO_LARGE} như lớp đầu.
     */
    static List<FieldError> imageErrors(List<ImageUpload> images) {
        List<FieldError> errors = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            check(errors, "full[" + i + "]", images.get(i).full());
            check(errors, "thumb[" + i + "]", images.get(i).thumb());
        }
        return errors;
    }

    private static void check(List<FieldError> errors, String field, byte[] b) {
        if (b != null && b.length > MAX_IMAGE_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Ảnh quá lớn (tối đa 1 MB).");
        }
        if (!Jpeg.isJpeg(b)) {
            errors.add(new FieldError(field, "Ảnh phải là JPEG"));
        }
    }

    private List<ItemImage> upload(UUID itemId, List<ImageUpload> images) {
        List<ItemImage> stored = new ArrayList<>();
        try {
            for (int pos = 0; pos < images.size(); pos++) {
                String full = "items/" + itemId + "/" + pos + ".jpg";
                String thumb = "items/" + itemId + "/" + pos + "_thumb.jpg";
                storage.upload(full, images.get(pos).full(), "image/jpeg");
                storage.upload(thumb, images.get(pos).thumb(), "image/jpeg");
                stored.add(new ItemImage(UUID.randomUUID(), itemId, pos, full, thumb));
            }
        } catch (RestClientException e) {
            log.warn("Upload ảnh tin {} thất bại", itemId, e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE",
                    "Không tải được ảnh lên, vui lòng thử lại.");
        }
        return stored;
    }
}
