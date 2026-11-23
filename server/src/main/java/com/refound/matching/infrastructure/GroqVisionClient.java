package com.refound.matching.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Groq chat completions (API kiểu OpenAI) với model vision (ADR-02, U8): {@code reasoning_effort="none"},
 * {@code response_format=json_object}, trả {@code {"mo_ta": "…"}}. Groq đứng sau Cloudflare, thiếu
 * {@code User-Agent} hợp lệ bị chặn 403 (docs/setup.md). Key hoặc model để trống thì tắt bước mô tả ảnh.
 */
@Component
public class GroqVisionClient implements VisionClient {

    static final String PROMPT = """
            Ảnh chụp một món đồ thất lạc trong trường đại học. Mô tả món đồ bằng MỘT câu tiếng Việt, tối đa 40 từ: \
            loại đồ, màu sắc, chất liệu, hãng nếu thấy logo, đặc điểm nhận dạng (vết xước, móc khóa, nhãn dán). \
            Không chép lại chữ hay số in trên đồ vật, không nêu tên người. \
            Chỉ trả JSON dạng {"mo_ta": "..."}.""";
    static final int MAX_WORDS = 40;
    static final int MAX_CHARS = 400;
    private static final Logger log = LoggerFactory.getLogger(GroqVisionClient.class);

    private final RestClient http;
    private final String model;
    private final ObjectMapper json;

    @Autowired
    public GroqVisionClient(
            RestClient.Builder builder,
            ObjectMapper json,
            @Value("${refound.ai.groq.url}") String url,
            @Value("${refound.ai.groq.key}") String key,
            @Value("${refound.ai.groq.model}") String model) {
        this(blank(key) ? null : builder.requestFactory(timeouts()).baseUrl(url)
                        .defaultHeader("Authorization", "Bearer " + key)
                        .defaultHeader("User-Agent", "refound-server/1.0")
                        .build(),
                json, blank(model) ? null : model.strip());
    }

    /** Cho test; {@code http} hoặc {@code model} null nghĩa là tắt. */
    GroqVisionClient(RestClient http, ObjectMapper json, String model) {
        this.http = http;
        this.json = json;
        this.model = model;
    }

    @Override
    public Optional<String> describe(List<byte[]> jpegs) {
        if (http == null || model == null || jpegs.isEmpty()) {
            return Optional.empty();
        }
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", PROMPT));
        for (byte[] jpeg : jpegs.subList(0, Math.min(MAX_IMAGES, jpegs.size()))) {
            content.add(Map.of("type", "image_url", "image_url",
                    Map.of("url", "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg))));
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "reasoning_effort", "none",
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.2,
                "max_completion_tokens", 200,
                "messages", List.of(Map.of("role", "user", "content", content)));
        try {
            JsonNode res = http.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(JsonNode.class);
            String raw = res == null ? null : res.at("/choices/0/message/content").asText(null);
            return raw == null ? Optional.empty() : caption(json.readTree(raw).path("mo_ta").asText(""));
        } catch (Exception e) { // lỗi mạng, 4xx/5xx, 429, JSON hỏng: đều bỏ qua mô tả ảnh (ADR-02)
            log.warn("Groq không mô tả được ảnh: {}", e.toString());
            return Optional.empty();
        }
    }

    /** Một dòng, tối đa 40 từ và 400 ký tự (model có thể trả dài hơn yêu cầu). */
    static Optional<String> caption(String text) {
        String s = text == null ? "" : text.replaceAll("\\s+", " ").strip();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        String[] words = s.split(" ");
        if (words.length > MAX_WORDS) {
            s = String.join(" ", List.of(words).subList(0, MAX_WORDS));
        }
        if (s.codePointCount(0, s.length()) > MAX_CHARS) { // cắt theo code point, không tách đôi emoji
            s = s.substring(0, s.offsetByCodePoints(0, MAX_CHARS)).strip();
        }
        return Optional.of(s);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static JdkClientHttpRequestFactory timeouts() {
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        f.setReadTimeout(Duration.ofSeconds(30));
        return f;
    }
}
