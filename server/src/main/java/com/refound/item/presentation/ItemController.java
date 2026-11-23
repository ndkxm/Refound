package com.refound.item.presentation;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.common.ApiException;
import com.refound.common.ApiException.FieldError;
import com.refound.common.CurrentUser;
import com.refound.common.GeoPoint;
import com.refound.common.LatLng;
import com.refound.item.application.ItemDtos.ItemOwnerDto;
import com.refound.item.application.ItemDtos.ItemSummaryDto;
import com.refound.item.application.ItemDtos.Page;
import com.refound.item.application.ItemDtos.SecretDto;
import com.refound.item.application.ItemQueryService;
import com.refound.item.application.ItemService;
import com.refound.item.application.ItemService.Edit;
import com.refound.item.application.ItemService.ImageUpload;
import com.refound.item.domain.Bbox;
import com.refound.item.domain.ItemRules;
import com.refound.item.domain.ItemRules.Draft;
import com.refound.item.domain.ItemStatus;
import com.refound.item.domain.ItemType;
import com.refound.item.infrastructure.ItemRepository.PublicFilter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** API tin (06 §6.2, FR-ITEM-01..05). userId và quyền admin lấy từ token + DB, không từ body (03 §3.3). */
@RestController
public class ItemController {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50; // 06 §6.1

    private final ItemService service;
    private final ItemQueryService query;
    private final ObjectMapper json;

    public ItemController(ItemService service, ItemQueryService query, ObjectMapper json) {
        this.service = service;
        this.query = query;
        this.json = json;
    }

    /**
     * FR-ITEM-01, 02. Multipart: {@code data} là chuỗi JSON (nhận cả khi phần này không có Content-Type, vì FormData
     * của React Native gửi chuỗi như text), {@code full} và {@code thumb} lặp lại 0–4 lần, cặp thứ i là ảnh vị trí i.
     */
    @PostMapping(path = "/api/v1/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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

    /**
     * FR-ITEM-03: tin {@code open} cho bản đồ và danh sách. {@code from}, {@code to} là ISO-8601 có múi giờ; dấu
     * {@code +} của múi giờ phải mã hóa thành {@code %2B} trong URL.
     */
    @GetMapping("/api/v1/items")
    public Page<ItemSummaryDto> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String bbox,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        ItemType t = null;
        if (type != null) {
            t = parseType(type);
            if (t == null) {
                throw invalid("type", "Loại tin phải là lost hoặc found");
            }
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw invalid("from", "from phải trước to");
        }
        PublicFilter filter = new PublicFilter(t, ItemRules.clean(category),
                from == null ? null : from.toInstant(), to == null ? null : to.toInstant(), Bbox.parse(bbox));
        return query.listPublic(filter, cursor, pageSize(limit));
    }

    @GetMapping("/api/v1/items/{id}")
    public Object detail(JwtAuthenticationToken auth, @PathVariable UUID id) {
        return query.detail(CurrentUser.viewer(auth), id);
    }

    /** FR-ITEM-05: tin của mình ở mọi trạng thái, trừ tin đã xóa. */
    @GetMapping("/api/v1/me/items")
    public Page<ItemOwnerDto> mine(
            JwtAuthenticationToken auth,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return query.mine(CurrentUser.viewer(auth), parseStatus(status), cursor, pageSize(limit));
    }

    @PatchMapping("/api/v1/items/{id}")
    public ItemOwnerDto edit(JwtAuthenticationToken auth, @PathVariable UUID id, @RequestBody EditItemRequest req) {
        return service.edit(CurrentUser.viewer(auth), id, req.toEdit());
    }

    @PostMapping("/api/v1/items/{id}/close")
    public ItemOwnerDto close(JwtAuthenticationToken auth, @PathVariable UUID id, @RequestBody CloseRequest req) {
        return service.close(CurrentUser.viewer(auth), id, ItemRules.clean(req.reason()));
    }

    @PostMapping("/api/v1/items/{id}/mark-found")
    public ItemOwnerDto markFound(JwtAuthenticationToken auth, @PathVariable UUID id) {
        return service.markFound(CurrentUser.viewer(auth), id);
    }

    @PostMapping("/api/v1/items/{id}/renew")
    public ItemOwnerDto renew(JwtAuthenticationToken auth, @PathVariable UUID id) {
        return service.renew(CurrentUser.viewer(auth), id);
    }

    @DeleteMapping("/api/v1/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(JwtAuthenticationToken auth, @PathVariable UUID id) {
        service.delete(CurrentUser.viewer(auth), id);
    }

    /** BR-02: chỉ tác giả tin {@code found}; người khác, kể cả admin, nhận 404 (X6). */
    @GetMapping("/api/v1/items/{id}/secret")
    public SecretDto secret(JwtAuthenticationToken auth, @PathVariable UUID id) {
        return query.secret(CurrentUser.viewer(auth), id);
    }

    // --- đọc dữ liệu vào ---

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
            throw invalid("thumb", "Mỗi ảnh cần đủ bản lớn và bản nhỏ");
        }
        if (f.size() > ItemRules.MAX_IMAGES) { // không đọc byte của 9 ảnh chỉ để báo lỗi
            throw invalid("full", "Tối đa " + ItemRules.MAX_IMAGES + " ảnh");
        }
        List<ImageUpload> images = new ArrayList<>();
        for (int i = 0; i < f.size(); i++) {
            images.add(new ImageUpload(f.get(i).getBytes(), t.get(i).getBytes()));
        }
        return images;
    }

    static int pageSize(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw invalid("limit", "limit từ 1 đến " + MAX_LIMIT);
        }
        return limit;
    }

    static ItemType parseType(String type) {
        return "lost".equals(type) ? ItemType.LOST : "found".equals(type) ? ItemType.FOUND : null;
    }

    private static ItemStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ItemStatus.fromDb(status);
        } catch (IllegalArgumentException e) {
            throw invalid("status", "Trạng thái không hợp lệ");
        }
    }

    private static ApiException invalid(String field, String message) {
        return ApiException.validation(List.of(new FieldError(field, message)));
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

    /** {@code PATCH /items/{id}}: trường không gửi là giữ nguyên. Không đổi được loại tin và ảnh. */
    record EditItemRequest(
            String description,
            String category,
            OffsetDateTime happenedAt,
            LatLng location,
            Integer radiusM,
            String question,
            String answer) {

        Edit toEdit() {
            GeoPoint point = null;
            if (location != null) {
                point = location.toPoint();
                if (point == null) {
                    throw invalid("location", "Vị trí cần đủ lat và lng");
                }
            }
            return new Edit(ItemRules.normalize(description), ItemRules.normalize(category),
                    happenedAt == null ? null : happenedAt.toInstant(), point, radiusM,
                    ItemRules.normalize(question), ItemRules.normalize(answer));
        }
    }

    record CloseRequest(String reason) {}
}
