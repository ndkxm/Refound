package com.refound.matching.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.refound.matching.domain.EmbeddingText;
import com.refound.matching.infrastructure.EmbeddingClient;
import com.refound.matching.infrastructure.EmbeddingClient.Input;
import com.refound.matching.infrastructure.GeminiEmbeddingClient;
import com.refound.matching.seed.EvalDataset;
import com.refound.matching.seed.JpegResize;
import com.refound.matching.seed.VectorCache;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.client.RestClient;

/**
 * Đo độ chính xác đối chiếu trên bộ mẫu (04 §4.6, FR-MATCH-05): {@code ./mvnw test -Dgroups=eval}.
 * Chỉ dùng cosine (chưa có vị trí, thời gian, danh mục), giống bước lấy ứng viên của {@code /search}. Vector lưu ở
 * {@code eval/cache-<model>.json}; thiếu vector thì cần {@code GEMINI_API_KEY}. Test chỉ in số, không chặn build.
 */
@Tag("eval")
@EnabledIfSystemProperty(named = "refound.groups", matches = ".*eval.*")
class MatchingEvalTest {

    static final Path DIR = Path.of("src/test/resources/eval");
    static final String MODEL = "gemini-embedding-2";

    /** Vector của một tin: chữ dạng tài liệu (để nạp demo), chữ dạng truy vấn (chỉ tin lost), ảnh. */
    record Vectors(float[] text, float[] query, List<float[]> images) {}

    @Test
    void report() throws Exception {
        Assumptions.assumeTrue(EvalDataset.exists(DIR), "Chưa có " + DIR.resolve(EvalDataset.FILE));
        ObjectMapper json = JsonMapper.builder().findAndAddModules().build();
        EvalDataset data = EvalDataset.load(DIR, json);
        Map<String, Cat> cats = categories();
        checkDataset(data, cats);

        // Gom mọi đầu vào cần embed, gọi một lượt qua cache
        Map<String, List<Input>> inputs = new LinkedHashMap<>();
        for (EvalDataset.Item it : data.items()) {
            Cat c = cats.get(it.category());
            List<Input> in = new ArrayList<>();
            in.add(new Input.Text(EmbeddingText.forItem(c.name(), it.description(), null)));
            in.add(new Input.Text(EmbeddingText.forQuery(it.description())));
            if (!c.sensitive()) {
                for (String img : it.images()) {
                    in.add(new Input.Image(JpegResize.maxSide(data.image(img), 512)));
                }
            }
            inputs.put(it.id(), in);
        }
        VectorCache cache = VectorCache.load(DIR, MODEL, json);
        List<Input> all = inputs.values().stream().flatMap(List::stream).toList();
        boolean complete = all.stream().allMatch(i -> cache.get(i).isPresent());
        String key = System.getenv("GEMINI_API_KEY");
        Assumptions.assumeTrue(complete || (key != null && !key.isBlank()),
                "Cache thiếu vector: đặt GEMINI_API_KEY để tính (tốn khoảng " + all.size() + " phần đầu vào)");
        EmbeddingClient gemini = complete ? null : new GeminiEmbeddingClient(RestClient.builder(),
                "https://generativelanguage.googleapis.com/v1beta", key, MODEL);
        cache.getOrEmbed(all, gemini);
        cache.saveIfChanged(json);

        Map<String, Vectors> v = new HashMap<>();
        inputs.forEach((id, in) -> {
            List<float[]> out = in.stream().map(i -> cache.get(i).orElseThrow()).toList();
            v.put(id, new Vectors(out.get(0), out.get(1), out.subList(2, out.size())));
        });

        List<EvalDataset.Item> found = data.items().stream().filter(EvalDataset.Item::found).toList();
        List<EvalDataset.Item> lost = data.items().stream().filter(i -> !i.found()).toList();

        StringBuilder out = new StringBuilder("\n=== Đánh giá đối chiếu (").append(MODEL).append(", ")
                .append(found.size()).append(" found, ").append(lost.size()).append(" lost) ===\n");
        out.append(String.format("%-28s %4s %7s %7s %7s%n", "Nhóm", "n", "top-1", "top-5", "MRR"));
        row(out, "chữ↔chữ", lost, found, (q, f) -> List.of(cos(v.get(q.id()).query(), v.get(f.id()).text())));
        row(out, "chữ↔ảnh", lost, found, (q, f) -> v.get(f.id()).images().stream()
                .map(img -> cos(v.get(q.id()).query(), img)).toList());
        row(out, "ảnh↔ảnh", lost, found, (q, f) -> {
            List<Double> s = new ArrayList<>();
            v.get(q.id()).images().forEach(a -> v.get(f.id()).images().forEach(b -> s.add(cos(a, b))));
            return s;
        });
        BiFunction<EvalDataset.Item, EvalDataset.Item, List<Double>> combined = (q, f) -> {
            List<float[]> qs = new ArrayList<>(v.get(q.id()).images());
            qs.add(v.get(q.id()).query());
            List<float[]> fs = new ArrayList<>(v.get(f.id()).images());
            fs.add(v.get(f.id()).text());
            List<Double> s = new ArrayList<>();
            qs.forEach(a -> fs.forEach(b -> s.add(cos(a, b))));
            return s;
        };
        row(out, "kết hợp (max, như /search)", lost, found, combined);

        // Phân bố cosine của cặp đúng / sai (kết hợp) để chọn c0, c1 (H01)
        List<Double> right = new ArrayList<>();
        List<Double> wrong = new ArrayList<>();
        for (EvalDataset.Item q : lost) {
            for (EvalDataset.Item f : found) {
                double s = combined.apply(q, f).stream().mapToDouble(Double::doubleValue).max().orElse(-1);
                (f.id().equals(q.expectedFoundId()) ? right : wrong).add(s);
            }
        }
        out.append(String.format("%nCosine cặp đúng: %s%nCosine cặp sai:  %s%n", spread(right), spread(wrong)));
        List<Long> ms = new ArrayList<>(cache.callMillis());
        ms.sort(null);
        out.append(ms.isEmpty()
                ? "Gemini: 0 request (đủ vector trong cache)\n"
                : String.format("Gemini: %d request, p50 %d ms (lô ≤ 50 phần)%n", ms.size(), ms.get(ms.size() / 2)));
        System.out.println(out);
    }

    private static void row(StringBuilder out, String name, List<EvalDataset.Item> lost, List<EvalDataset.Item> found,
            BiFunction<EvalDataset.Item, EvalDataset.Item, List<Double>> pairScores) {
        List<Integer> ranks = new ArrayList<>();
        for (EvalDataset.Item q : lost) {
            Map<String, Double> scores = new HashMap<>();
            for (EvalDataset.Item f : found) {
                pairScores.apply(q, f).stream().mapToDouble(Double::doubleValue).max()
                        .ifPresent(s -> scores.put(f.id(), s));
            }
            if (scores.containsKey(q.expectedFoundId())) { // nhóm không áp dụng (không có ảnh) thì bỏ truy vấn
                ranks.add(RankingMetrics.rank(scores, q.expectedFoundId()));
            }
        }
        RankingMetrics m = RankingMetrics.of(ranks);
        out.append(String.format("%-28s %4d %6.1f%% %6.1f%% %7.3f%n", name, m.n(), m.top1() * 100, m.top5() * 100, m.mrr()));
    }

    private static String spread(List<Double> xs) {
        if (xs.isEmpty()) {
            return "(không có)";
        }
        List<Double> s = new ArrayList<>(xs);
        s.sort(null);
        return String.format("min %.3f · p10 %.3f · p50 %.3f · p90 %.3f · max %.3f", s.getFirst(),
                s.get(s.size() / 10), s.get(s.size() / 2), s.get(s.size() * 9 / 10), s.getLast());
    }

    static double cos(float[] a, float[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / Math.sqrt(na * nb);
    }

    private static void checkDataset(EvalDataset data, Map<String, Cat> cats) {
        List<String> ids = data.items().stream().map(EvalDataset.Item::id).toList();
        assertThat(ids).as("id trùng").doesNotHaveDuplicates();
        List<String> foundIds = data.items().stream().filter(EvalDataset.Item::found).map(EvalDataset.Item::id).toList();
        for (EvalDataset.Item it : data.items()) {
            assertThat(it.type()).as(it.id() + ".type").isIn("found", "lost");
            assertThat(cats).as(it.id() + ".category").containsKey(it.category());
            assertThat(it.description().strip().length()).as(it.id() + ".description 5–1000").isBetween(5, 1000);
            assertThat(it.images()).as(it.id() + ".images tối đa 4").hasSizeLessThanOrEqualTo(4);
            it.images().forEach(img -> assertThat(DIR.resolve("images").resolve(img)).as(it.id()).exists());
            if (it.found()) {
                assertThat(it.images()).as(it.id() + ": tin found cần ảnh").isNotEmpty();
            } else {
                assertThat(foundIds).as(it.id() + ".expectedFoundId").contains(it.expectedFoundId());
            }
        }
    }

    record Cat(String name, boolean sensitive) {}

    /** Đọc danh mục từ migration P0 (nguồn duy nhất), để chữ embed giống hệt server và lệnh --seed. */
    private static Map<String, Cat> categories() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V202611230900__core_p0.sql"));
        Matcher m = Pattern.compile("\\('(\\w+)',\\s*'([^']+)',\\s*'\\w+',\\s*(true|false)\\)").matcher(sql);
        Map<String, Cat> out = new HashMap<>();
        while (m.find()) {
            out.put(m.group(1), new Cat(m.group(2), Boolean.parseBoolean(m.group(3))));
        }
        assertThat(out).hasSize(14);
        return out;
    }
}
