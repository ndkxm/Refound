package com.refound.item.application;

import com.refound.common.ApiException;
import com.refound.common.ApiException.FieldError;
import com.refound.common.Jpeg;
import com.refound.common.Viewer;
import com.refound.common.storage.StorageClient;
import com.refound.item.application.ItemDtos.ItemOwnerDto;
import com.refound.item.domain.Category;
import com.refound.item.domain.Item;
import com.refound.item.domain.ItemImage;
import com.refound.item.domain.ItemRules;
import com.refound.item.domain.ItemRules.Draft;
import com.refound.item.domain.ItemStatus;
import com.refound.item.domain.ItemType;
import com.refound.item.domain.LocationRounding;
import com.refound.item.domain.PostingLimit;
import com.refound.item.infrastructure.CategoryRepository;
import com.refound.item.infrastructure.ItemRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

/** Tạo tin (I1, FR-ITEM-01, 02). */
@Service
public class ItemService {

    static final int MAX_IMAGE_BYTES = 1024 * 1024; // ADR-06
    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository items;
    private final CategoryRepository categories;
    private final StorageClient storage;
    private final ItemViews views;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ItemService(ItemRepository items, CategoryRepository categories, StorageClient storage, ItemViews views,
            TransactionTemplate tx, Clock clock) {
        this.items = items;
        this.categories = categories;
        this.storage = storage;
        this.views = views;
        this.tx = tx;
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
        });
        return views.owner(item, stored);
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
