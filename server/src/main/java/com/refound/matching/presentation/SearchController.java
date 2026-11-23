package com.refound.matching.presentation;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.common.ApiException;
import com.refound.common.ApiException.FieldError;
import com.refound.common.GeoPoint;
import com.refound.common.LatLng;
import com.refound.item.domain.ItemRules;
import com.refound.item.domain.ItemType;
import com.refound.matching.application.SearchService;
import com.refound.matching.application.SearchService.SearchQuery;
import com.refound.matching.application.SearchService.SearchResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SearchController {

    private final SearchService search;
    private final ObjectMapper json;

    public SearchController(SearchService search, ObjectMapper json) {
        this.search = search;
        this.json = json;
    }

    /**
     * FR-MATCH-01 (06 §6.2). Multipart: {@code data} là chuỗi JSON {@code {type, text?, location?, radiusM?,
     * happenedAt?, category?}} (không cần Content-Type, như {@code POST /items}), {@code image} là JPEG ≤ 1 MB.
     */
    @PostMapping(path = "/api/v1/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SearchResponse search(
            @RequestPart("data") String data, @RequestPart(name = "image", required = false) MultipartFile image)
            throws IOException {
        SearchRequest req;
        try {
            req = json.readValue(data, SearchRequest.class);
        } catch (JacksonException e) {
            throw ApiException.validation("Dữ liệu tìm kiếm không đúng định dạng JSON.");
        }
        if (req == null) {
            throw ApiException.validation("Thiếu dữ liệu tìm kiếm.");
        }
        return search.search(req.toQuery(image == null || image.isEmpty() ? null : image.getBytes()));
    }

    record SearchRequest(
            String type,
            String text,
            LatLng location,
            Integer radiusM,
            OffsetDateTime happenedAt,
            String category) {

        SearchQuery toQuery(byte[] image) {
            GeoPoint point = null;
            if (location != null) {
                point = location.toPoint();
                if (point == null) {
                    throw ApiException.validation(List.of(new FieldError("location", "Vị trí cần đủ lat và lng")));
                }
            }
            ItemType t = "lost".equals(type) ? ItemType.LOST : "found".equals(type) ? ItemType.FOUND : null;
            return new SearchQuery(t, ItemRules.clean(text), point, radiusM,
                    happenedAt == null ? null : happenedAt.toInstant(), ItemRules.clean(category), image);
        }
    }
}
