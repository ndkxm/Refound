package com.refound.matching.application;

import com.refound.common.ApiException;
import com.refound.common.ApiException.FieldError;
import com.refound.common.GeoPoint;
import com.refound.common.Jpeg;
import com.refound.item.application.ItemDtos.ItemSummaryDto;
import com.refound.item.application.ItemQueryService;
import com.refound.item.domain.Category;
import com.refound.item.domain.ItemType;
import com.refound.item.infrastructure.CategoryRepository;
import com.refound.matching.domain.EmbeddingText;
import com.refound.matching.domain.KeywordFilter;
import com.refound.matching.domain.MatchReasons;
import com.refound.matching.domain.MatchScorer;
import com.refound.matching.domain.MatchScorer.Components;
import com.refound.matching.domain.MatchScorer.Input;
import com.refound.matching.domain.MatchScorer.Score;
import com.refound.matching.domain.MatchWeights;
import com.refound.matching.infrastructure.EmbeddingClient;
import com.refound.matching.infrastructure.EmbeddingClient.EmbeddingException;
import com.refound.matching.infrastructure.SearchRepository;
import com.refound.matching.infrastructure.SearchRepository.Candidate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * {@code POST /search} (04 §4.3, FR-MATCH-01, 02): embed câu truy vấn (cache trước) → SQL lọc cứng + max cosine lấy
 * 50 ứng viên → {@link MatchScorer} → 20 kết quả kèm lý do. Gemini lỗi: tìm bằng chữ thì hạ cấp sang lọc từ khóa
 * ({@code degraded=true}), chỉ có ảnh thì 503 {@code AI_UNAVAILABLE}.
 */
@Service
public class SearchService {

    static final int CANDIDATES = 50;
    static final int RESULTS = 20;
    static final int MAX_TEXT = 500;
    static final int MAX_RADIUS_M = 2000;
    static final Duration TIME_TOLERANCE = Duration.ofHours(24); // 4.2: nhặt trước lúc mất tối đa 1 ngày
    static final Duration CLOCK_SKEW = Duration.ofMinutes(10);
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final SearchRepository repo;
    private final EmbeddingClient embeddings;
    private final AiCircuit circuit;
    private final CategoryRepository categories;
    private final ItemQueryService items;
    private final MatchWeights weights;
    private final Clock clock;

    public SearchService(SearchRepository repo, EmbeddingClient embeddings, AiCircuit circuit,
            CategoryRepository categories, ItemQueryService items, MatchWeights weights, Clock clock) {
        this.repo = repo;
        this.embeddings = embeddings;
        this.circuit = circuit;
        this.categories = categories;
        this.items = items;
        this.weights = weights;
        this.clock = clock;
    }

    /**
     * @param type loại tin cần tìm (Owner tìm {@code found}, Finder tìm {@code lost})
     * @param text chữ đã bỏ khoảng trắng, null nếu không có
     * @param radiusM bán kính khu vực mất của người tìm (khi tìm {@code found})
     * @param happenedAt lúc mất (tìm {@code found}) hoặc lúc nhặt (tìm {@code lost})
     */
    public record SearchQuery(
            ItemType type, String text, GeoPoint location, Integer radiusM, Instant happenedAt, String category,
            byte[] image) {}

    public record Result(ItemSummaryDto item, double score, Components components, List<String> reasons) {}

    public record SearchResponse(List<Result> results, boolean degraded) {}

    public SearchResponse search(SearchQuery q) {
        Category category = validate(q);
        List<float[]> vectors;
        boolean degraded = false;
        try {
            vectors = queryVectors(q);
        } catch (AiUnavailable e) {
            if (q.text() == null) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                        "Tìm kiếm bằng ảnh tạm thời không khả dụng, vui lòng tìm bằng chữ.");
            }
            vectors = List.of();
            degraded = true;
        }

        Instant from = null;
        Instant to = null;
        if (q.happenedAt() != null) { // lọc cứng 4.2: F.happened_at ≥ L.happened_at − 24 h
            if (q.type() == ItemType.FOUND) {
                from = q.happenedAt().minus(TIME_TOLERANCE);
            } else {
                to = q.happenedAt().plus(TIME_TOLERANCE);
            }
        }
        List<Candidate> candidates = repo.candidates(new SearchRepository.Query(q.type(), vectors, embeddings.model(),
                q.location(), from, to, degraded ? KeywordFilter.patterns(q.text()) : List.of()), CANDIDATES);

        record Scored(Candidate c, Score s, Input in) {}
        List<Scored> scored = new ArrayList<>();
        MatchScorer.Category queryCategory = category == null ? null : new MatchScorer.Category(category.code(), category.groupCode());
        for (Candidate c : candidates) {
            Input in = new Input(c.cosine(), c.distanceM(),
                    q.type() == ItemType.FOUND ? q.radiusM() : c.radiusM(),
                    deltaHours(q, c), queryCategory, new MatchScorer.Category(c.category(), c.groupCode()));
            scored.add(new Scored(c, MatchScorer.score(in, weights), in));
        }
        scored.sort(Comparator.comparingDouble((Scored x) -> x.s().total()).reversed()
                .thenComparing(x -> x.c().happenedAt(), Comparator.reverseOrder()));
        List<Scored> top = scored.subList(0, Math.min(RESULTS, scored.size()));

        Map<UUID, ItemSummaryDto> summaries = items.summaries(top.stream().map(x -> x.c().id()).toList());
        List<Result> results = new ArrayList<>();
        for (Scored x : top) {
            ItemSummaryDto item = summaries.get(x.c().id());
            if (item != null) { // tin vừa bị xóa giữa hai truy vấn
                results.add(new Result(item, round(x.s().total()), rounded(x.s().components()),
                        MatchReasons.of(x.s().components(), x.in(), "done".equals(x.c().embeddingStatus()))));
            }
        }
        return new SearchResponse(results, degraded);
    }

    /** Giờ từ lúc mất tới lúc nhặt: tìm {@code found} thì người tìm là bên mất, ngược lại người tìm là bên nhặt. */
    private static Double deltaHours(SearchQuery q, Candidate c) {
        if (q.happenedAt() == null) {
            return null;
        }
        Instant lost = q.type() == ItemType.FOUND ? q.happenedAt() : c.happenedAt();
        Instant found = q.type() == ItemType.FOUND ? c.happenedAt() : q.happenedAt();
        return Duration.between(lost, found).toMillis() / 3_600_000.0;
    }

    /** Vector chữ và/hoặc ảnh của câu truy vấn; lấy từ cache trước, phần thiếu embed trong một lần gọi. */
    private List<float[]> queryVectors(SearchQuery q) {
        List<EmbeddingClient.Input> inputs = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        String model = embeddings.model();
        if (q.text() != null) {
            String text = EmbeddingText.forQuery(q.text());
            inputs.add(new EmbeddingClient.Input.Text(text));
            keys.add(sha256(model + "\ntext\n" + text));
        }
        if (q.image() != null) {
            inputs.add(new EmbeddingClient.Input.Image(q.image()));
            keys.add(sha256(model + "\nimage\n" + HexFormat.of().formatHex(digest(q.image()))));
        }
        if (inputs.isEmpty()) {
            return List.of();
        }
        Map<String, float[]> cached = repo.cached(keys);
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            if (!cached.containsKey(keys.get(i))) {
                missing.add(i);
            }
        }
        if (!missing.isEmpty()) {
            if (circuit.isOpen()) {
                throw new AiUnavailable();
            }
            try {
                List<float[]> fresh = embeddings.embed(missing.stream().map(inputs::get).toList());
                for (int j = 0; j < missing.size(); j++) {
                    String key = keys.get(missing.get(j));
                    cached.put(key, fresh.get(j));
                    repo.cache(key, fresh.get(j));
                }
            } catch (EmbeddingException e) {
                if (e.rateLimited()) {
                    circuit.pause();
                }
                log.warn("Embed câu tìm kiếm lỗi, hạ cấp: {}", e.getMessage());
                throw new AiUnavailable();
            }
        }
        return keys.stream().map(cached::get).toList();
    }

    private Category validate(SearchQuery q) {
        List<FieldError> errors = new ArrayList<>();
        if (q.type() == null) {
            errors.add(new FieldError("type", "Loại tin cần tìm phải là lost hoặc found"));
        }
        if (q.text() != null && q.text().codePointCount(0, q.text().length()) > MAX_TEXT) {
            errors.add(new FieldError("text", "Nội dung tìm tối đa " + MAX_TEXT + " ký tự"));
        }
        if (q.location() != null && !q.location().isValid()) {
            errors.add(new FieldError("location", "Vị trí không hợp lệ"));
        }
        if (q.radiusM() != null && (q.radiusM() < 0 || q.radiusM() > MAX_RADIUS_M)) {
            errors.add(new FieldError("radiusM", "Bán kính từ 0 đến " + MAX_RADIUS_M + " m"));
        }
        if (q.happenedAt() != null && q.happenedAt().isAfter(clock.instant().plus(CLOCK_SKEW))) {
            errors.add(new FieldError("happenedAt", "Thời điểm không được ở tương lai"));
        }
        Category category = q.category() == null ? null : categories.find(q.category()).orElse(null);
        if (q.category() != null && category == null) {
            errors.add(new FieldError("category", "Danh mục không hợp lệ"));
        }
        if (q.image() != null) {
            if (q.image().length > 1024 * 1024) {
                throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Ảnh quá lớn (tối đa 1 MB).");
            }
            if (!Jpeg.isJpeg(q.image())) {
                errors.add(new FieldError("image", "Ảnh phải là JPEG"));
            }
        }
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
        return category;
    }

    private static Components rounded(Components c) {
        return new Components(round(c.v()), round(c.d()), round(c.t()), round(c.c()));
    }

    private static double round(double x) {
        return Math.round(x * 1000) / 1000.0;
    }

    private static String sha256(String s) {
        return HexFormat.of().formatHex(digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] digest(byte[] b) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(b);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Gemini đang tạm dừng hoặc vừa lỗi. */
    private static final class AiUnavailable extends RuntimeException {
        AiUnavailable() {
            super(null, null, false, false);
        }
    }
}
