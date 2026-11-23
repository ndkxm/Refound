package com.refound.item.application;

import static com.refound.common.Times.vn;

import com.refound.common.storage.StorageClient;
import com.refound.item.application.ItemDtos.ImageDto;
import com.refound.item.application.ItemDtos.ItemOwnerDto;
import com.refound.item.application.ItemDtos.ItemPublicDto;
import com.refound.item.application.ItemDtos.ItemSummaryDto;
import com.refound.item.domain.Item;
import com.refound.item.domain.ItemAccess;
import com.refound.item.domain.ItemAccess.View;
import com.refound.item.domain.ItemImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/** Dựng DTO theo mức xem và ký URL ảnh theo lô một lần gọi Storage cho mỗi response (ADR-06, U12). */
@Component
public class ItemViews {

    static final Duration URL_TTL = Duration.ofHours(1);
    private static final Logger log = LoggerFactory.getLogger(ItemViews.class);

    private final StorageClient storage;

    public ItemViews(StorageClient storage) {
        this.storage = storage;
    }

    public ItemOwnerDto owner(Item item, List<ItemImage> images) {
        return toOwner(item, imageDtos(images, sign(allPaths(images))));
    }

    /** Chi tiết tin theo mức xem ({@link ItemAccess#requireView}). */
    public Object detail(Item item, View view, List<ItemImage> images) {
        return view == View.FULL ? owner(item, images) : publicView(item, images);
    }

    /** Nhiều tin của chủ (trang {@code /me/items}): một lần ký cho cả trang. */
    public List<ItemOwnerDto> owners(List<Item> items, Map<UUID, List<ItemImage>> imagesByItem) {
        List<ItemImage> all = items.stream().flatMap(i -> imagesOf(imagesByItem, i).stream()).toList();
        Map<String, String> urls = sign(allPaths(all));
        return items.stream().map(i -> toOwner(i, imageDtos(imagesOf(imagesByItem, i), urls))).toList();
    }

    public ItemPublicDto publicView(Item item, List<ItemImage> images) {
        List<ItemImage> visible = ItemAccess.imagesVisible(View.PUBLIC, item.sensitive()) ? images : List.of();
        return new ItemPublicDto(item.id(), item.authorId(), item.type().db(), item.status().db(), item.category(),
                item.sensitive(), item.description(), vn(item.happenedAt()), item.publicLocation(), item.radiusM(),
                imageDtos(visible, sign(allPaths(visible))), vn(item.createdAt()));
    }

    /** Dòng danh sách công khai: ảnh nhỏ vị trí 0, trừ tin nhạy cảm (BR-08). Một lần ký cho cả trang. */
    public List<ItemSummaryDto> summaries(List<Item> items, Map<UUID, List<ItemImage>> imagesByItem) {
        Map<UUID, String> thumbPath = new HashMap<>();
        for (Item i : items) {
            List<ItemImage> imgs = imagesOf(imagesByItem, i);
            if (!imgs.isEmpty() && ItemAccess.imagesVisible(View.PUBLIC, i.sensitive())) {
                thumbPath.put(i.id(), imgs.getFirst().thumbPath());
            }
        }
        Map<String, String> urls = sign(thumbPath.values());
        return items.stream()
                .map(i -> new ItemSummaryDto(i.id(), i.type().db(), i.status().db(), i.category(), i.sensitive(),
                        i.description(), vn(i.happenedAt()), i.publicLocation(), i.radiusM(),
                        thumbPath.containsKey(i.id()) ? urls.get(thumbPath.get(i.id())) : null, vn(i.createdAt())))
                .toList();
    }

    private static List<ItemImage> imagesOf(Map<UUID, List<ItemImage>> imagesByItem, Item item) {
        return imagesByItem.getOrDefault(item.id(), List.of());
    }

    private static ItemOwnerDto toOwner(Item item, List<ImageDto> images) {
        return new ItemOwnerDto(item.id(), item.authorId(), item.type().db(), item.status().db(), item.category(),
                item.sensitive(), item.description(), vn(item.happenedAt()), item.location(), item.publicLocation(),
                item.radiusM(), images, item.embeddingStatus(), item.closedReason(), vn(item.createdAt()),
                vn(item.updatedAt()), vn(item.lastActivityAt()));
    }

    private static List<ImageDto> imageDtos(List<ItemImage> images, Map<String, String> urls) {
        return images.stream()
                .map(img -> new ImageDto(img.id(), img.position(), urls.get(img.fullPath()), urls.get(img.thumbPath())))
                .toList();
    }

    private static List<String> allPaths(List<ItemImage> images) {
        List<String> paths = new ArrayList<>();
        for (ItemImage img : images) {
            paths.add(img.fullPath());
            paths.add(img.thumbPath());
        }
        return paths;
    }

    /** Storage lỗi khi ký thì trả tin không có URL ảnh, không làm hỏng cả request (như ảnh đại diện ở P0-02). */
    private Map<String, String> sign(Collection<String> paths) {
        if (paths.isEmpty()) {
            return Map.of();
        }
        try {
            return storage.signedUrls(paths, URL_TTL);
        } catch (RestClientException e) {
            log.warn("Không ký được {} URL ảnh", paths.size(), e);
            return Map.of();
        }
    }
}
