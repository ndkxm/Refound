package com.refound.item.presentation;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.common.ApiException;
import com.refound.common.ApiException.FieldError;
import com.refound.common.CurrentUser;
import com.refound.common.GeoPoint;
import com.refound.item.application.ItemDtos.ItemOwnerDto;
import com.refound.item.application.ItemService;
import com.refound.item.application.ItemService.ImageUpload;
import com.refound.item.domain.ItemRules;
import com.refound.item.domain.ItemRules.Draft;
import com.refound.item.domain.ItemType;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService service;
    private final ObjectMapper json;

    public ItemController(ItemService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    /**
     * FR-ITEM-01, 02 (06 §6.2). Multipart: {@code data} là chuỗi JSON (nhận cả khi phần này không có Content-Type,
     * vì FormData của React Native gửi chuỗi như text), {@code full} và {@code thumb} lặp lại 0–4 lần, cặp thứ i là
     * hai bản của ảnh vị trí i.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ItemOwnerDto create(
            JwtAuthenticationToken auth,
            @RequestPart("data") String data,
            @RequestPart(name = "full", required = false) List<MultipartFile> full,
            @RequestPart(name = "thumb", required = false) List<MultipartFile> thumb)
            throws IOException {
        CreateItemRequest req = parse(data);
        return service.create(CurrentUser.viewer(auth), req.toDraft(), pairs(full, thumb));
    }

    private CreateItemRequest parse(String data) {
        try {
            CreateItemRequest req = json.readValue(data, CreateItemRequest.class);
            if (req == null) {
                throw ApiException.validation("Thiếu dữ liệu tin.");
            }
            return req;
        } catch (JacksonException e) {
            throw ApiException.validation("Dữ liệu tin không đúng định dạng JSON.");
        }
    }

    private static List<ImageUpload> pairs(List<MultipartFile> full, List<MultipartFile> thumb) throws IOException {
        List<MultipartFile> f = full == null ? List.of() : full;
        List<MultipartFile> t = thumb == null ? List.of() : thumb;
        if (f.size() != t.size()) {
            throw ApiException.validation(List.of(new FieldError("thumb", "Mỗi ảnh cần đủ bản lớn và bản nhỏ")));
        }
        if (f.size() > ItemRules.MAX_IMAGES) { // không đọc byte của 9 ảnh chỉ để báo lỗi
            throw ApiException.validation(List.of(new FieldError("full", "Tối đa " + ItemRules.MAX_IMAGES + " ảnh")));
        }
        List<ImageUpload> images = new ArrayList<>();
        for (int i = 0; i < f.size(); i++) {
            images.add(new ImageUpload(f.get(i).getBytes(), t.get(i).getBytes()));
        }
        return images;
    }

    /** Phần {@code data} của {@code POST /items}. Số không gửi là null, không thành 0. */
    record CreateItemRequest(
            String type,
            String category,
            String description,
            OffsetDateTime happenedAt,
            LatLng location,
            Integer radiusM,
            String question,
            String answer) {

        Draft toDraft() {
            return new Draft(parseType(type), ItemRules.clean(category), ItemRules.clean(description),
                    happenedAt == null ? null : happenedAt.toInstant(), location == null ? null : location.toPoint(),
                    radiusM, ItemRules.clean(question), ItemRules.clean(answer));
        }
    }

    /** {@code {"lat", "lng"}}; thiếu một trong hai thì coi như không có vị trí (không âm thầm thành 0). */
    record LatLng(Double lat, Double lng) {

        GeoPoint toPoint() {
            return lat == null || lng == null ? null : new GeoPoint(lat, lng);
        }
    }

    static ItemType parseType(String type) {
        return "lost".equals(type) ? ItemType.LOST : "found".equals(type) ? ItemType.FOUND : null;
    }
}
