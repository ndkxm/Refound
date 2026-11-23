package com.refound.matching.infrastructure;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Gemini API {@code models/{model}:batchEmbedContents}: mỗi phần (chữ hoặc ảnh) là một request riêng trong lô nên ra
 * một vector riêng (gộp nhiều phần vào một {@code content} sẽ ra một vector tổng hợp). Đã gọi thật ngày 23/11/2026:
 * chữ + ảnh JPEG trong một lô, mỗi phần 768 chiều, độ dài 1 (U3).
 */
@Component
public class GeminiEmbeddingClient implements EmbeddingClient {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestClient http;
    private final String model;

    @Autowired
    public GeminiEmbeddingClient(
            RestClient.Builder builder,
            @Value("${refound.ai.gemini.url}") String url,
            @Value("${refound.ai.gemini.key}") String key,
            @Value("${refound.ai.gemini.model}") String model) {
        this(builder.requestFactory(timeouts()).baseUrl(url).defaultHeader("x-goog-api-key", requireKey(key)).build(),
                model);
    }

    /** Cho test: {@code http} dựng từ builder đã gắn {@code MockRestServiceServer}. */
    GeminiEmbeddingClient(RestClient http, String model) {
        this.http = http;
        this.model = model;
    }

    private static String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Thiếu GEMINI_API_KEY"); // dừng lúc khởi động thay vì mọi tin đều failed
        }
        return key;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public List<float[]> embed(List<Input> inputs) {
        List<Map<String, Object>> requests = new ArrayList<>();
        for (Input in : inputs) {
            Map<String, Object> part = switch (in) {
                case Input.Text t -> Map.of("text", t.text());
                case Input.Image i -> Map.of("inline_data",
                        Map.of("mime_type", "image/jpeg", "data", Base64.getEncoder().encodeToString(i.jpeg())));
            };
            requests.add(Map.of("model", "models/" + model, "content", Map.of("parts", List.of(part)),
                    "output_dimensionality", DIMENSIONS));
        }
        BatchResponse res;
        try {
            res = http.post()
                    .uri("/models/{model}:batchEmbedContents", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("requests", requests))
                    .retrieve()
                    .body(BatchResponse.class);
        } catch (HttpStatusCodeException e) {
            boolean limited = e.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value();
            throw new EmbeddingException("Gemini trả " + e.getStatusCode().value(), limited, e);
        } catch (RestClientException e) {
            throw new EmbeddingException("Không gọi được Gemini: " + e.getMessage(), false, e);
        }
        return vectors(res, inputs.size());
    }

    private static List<float[]> vectors(BatchResponse res, int expected) {
        if (res == null || res.embeddings() == null || res.embeddings().size() != expected) {
            throw new EmbeddingException("Gemini trả sai số vector", false, null);
        }
        List<float[]> out = new ArrayList<>();
        for (Embedding e : res.embeddings()) {
            if (e == null || e.values() == null || e.values().length != DIMENSIONS) {
                throw new EmbeddingException("Gemini trả vector sai số chiều", false, null);
            }
            out.add(e.values());
        }
        return out;
    }

    private static JdkClientHttpRequestFactory timeouts() {
        JdkClientHttpRequestFactory f =
                new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        f.setReadTimeout(READ_TIMEOUT);
        return f;
    }

    record BatchResponse(List<Embedding> embeddings) {}

    record Embedding(float[] values) {}
}
