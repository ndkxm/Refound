package com.refound.item.application;

import com.refound.common.ApiException;
import com.refound.common.Cursor;
import com.refound.common.Viewer;
import com.refound.item.application.ItemDtos.ItemOwnerDto;
import com.refound.item.application.ItemDtos.ItemSummaryDto;
import com.refound.item.application.ItemDtos.Page;
import com.refound.item.application.ItemDtos.SecretDto;
import com.refound.item.domain.Item;
import com.refound.item.domain.ItemAccess;
import com.refound.item.domain.ItemAccess.View;
import com.refound.item.domain.ItemImage;
import com.refound.item.domain.ItemStatus;
import com.refound.item.infrastructure.ItemRepository;
import com.refound.item.infrastructure.ItemRepository.PublicFilter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Đọc tin (FR-ITEM-03, 04, 05; BR-02). Module khác đọc tin qua lớp này, không qua repository (08 §8.2). */
@Service
public class ItemQueryService {

    private final ItemRepository items;
    private final ItemViews views;

    public ItemQueryService(ItemRepository items, ItemViews views) {
        this.items = items;
        this.views = views;
    }

    /** {@code ItemOwnerDto} cho tác giả và admin, {@code ItemPublicDto} cho người khác; không được thấy → 404 (X3, X4, X5). */
    public Object detail(Viewer me, UUID id) {
        Item item = items.findById(id).orElseThrow(ItemQueryService::notFound);
        View view = ItemAccess.requireView(item.facts(), me);
        return views.detail(item, view, items.findImages(List.of(id)));
    }

    public Page<ItemSummaryDto> listPublic(PublicFilter filter, String cursor, int limit) {
        List<Item> rows = items.listPublic(filter, Cursor.decode(cursor), limit + 1);
        List<Item> page = rows.subList(0, Math.min(limit, rows.size()));
        String next = rows.size() > limit ? new Cursor(page.getLast().happenedAt(), page.getLast().id()).encode() : null;
        return new Page<>(views.summaries(page, imagesOf(page)), next);
    }

    public Page<ItemOwnerDto> mine(Viewer me, ItemStatus status, String cursor, int limit) {
        List<Item> rows = items.listByAuthor(me.id(), status, Cursor.decode(cursor), limit + 1);
        List<Item> page = rows.subList(0, Math.min(limit, rows.size()));
        String next = rows.size() > limit ? new Cursor(page.getLast().createdAt(), page.getLast().id()).encode() : null;
        return new Page<>(views.owners(page, imagesOf(page)), next);
    }

    /** BR-02, X6: chỉ tác giả tin {@code found}; mọi người khác nhận 404 như tin không có câu hỏi. */
    public SecretDto secret(Viewer me, UUID id) {
        Item item = items.findById(id).orElseThrow(ItemQueryService::notFound);
        ItemAccess.requireSecretReader(item.facts(), me);
        return items.findSecret(id).map(s -> new SecretDto(s.question(), s.answer()))
                .orElseThrow(ItemQueryService::notFound);
    }

    /**
     * Dòng danh sách cho các tin {@code ids} (tìm kiếm, đối chiếu), giữ thứ tự của {@code ids}, bỏ id không còn.
     * Không kiểm quyền: người gọi đã chỉ chọn tin công khai ({@code open}, chưa xóa) trong SQL của mình.
     */
    public Map<UUID, ItemSummaryDto> summaries(List<UUID> ids) {
        Map<UUID, Item> byId = items.findByIds(ids).stream().collect(Collectors.toMap(Item::id, i -> i));
        List<Item> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        List<ItemSummaryDto> dtos = views.summaries(ordered, imagesOf(ordered));
        Map<UUID, ItemSummaryDto> out = new java.util.LinkedHashMap<>();
        dtos.forEach(d -> out.put(d.id(), d));
        return out;
    }

    private Map<UUID, List<ItemImage>> imagesOf(List<Item> page) {
        return items.findImages(page.stream().map(Item::id).toList()).stream()
                .collect(Collectors.groupingBy(ItemImage::itemId));
    }

    static ApiException notFound() {
        return ApiException.notFound("Không tìm thấy tin.");
    }
}
