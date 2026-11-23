package com.refound.item.application;

import static com.refound.common.Times.vn;

import com.refound.common.storage.StorageClient;
import com.refound.item.application.ItemDtos.ImageDto;
import com.refound.item.application.ItemDtos.ItemOwnerDto;
import com.refound.item.domain.Item;
import com.refound.item.domain.ItemImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
