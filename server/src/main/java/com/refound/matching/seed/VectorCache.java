package com.refound.matching.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.matching.infrastructure.EmbeddingClient;
import com.refound.matching.infrastructure.EmbeddingClient.Input;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Vector đã tính của bộ mẫu, lưu ở {@code eval/cache-<model>.json} (04 §4.6): chạy lại đánh giá và nạp demo không tốn
 * request Gemini. Khóa là sha256 của chữ hoặc của ảnh 512px; xóa file để tính lại.
 */
public final class VectorCache {

    static final int BATCH = 50;

    private final Path file;
    private final Map<String, float[]> vectors;
    private final List<Long> callMillis = new ArrayList<>();
    private boolean dirty;

    private VectorCache(Path file, Map<String, float[]> vectors) {
        this.file = file;
        this.vectors = vectors;
    }

    public static Path file(Path dir, String model) {
        return dir.resolve("cache-" + model + ".json");
    }

    public static VectorCache load(Path dir, String model, ObjectMapper json) {
        Path f = file(dir, model);
        if (!Files.exists(f)) {
            return new VectorCache(f, new TreeMap<>());
        }
        try {
            return new VectorCache(f, json.readValue(f.toFile(), new TypeReference<TreeMap<String, float[]>>() {}));
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được " + f, e);
        }
    }

    public static String key(Input input) {
        return switch (input) {
            case Input.Text t -> "text:" + sha256(t.text().getBytes(StandardCharsets.UTF_8));
            case Input.Image i -> "image:" + sha256(i.jpeg());
        };
    }

    public Optional<float[]> get(Input input) {
        return Optional.ofNullable(vectors.get(key(input)));
    }

    /** Lấy vector cho mọi đầu vào; phần chưa có thì gọi {@code client} theo lô {@value #BATCH}. */
    public List<float[]> getOrEmbed(List<Input> inputs, EmbeddingClient client) {
        List<Input> missing = inputs.stream().filter(i -> !vectors.containsKey(key(i))).distinct().toList();
        for (int from = 0; from < missing.size(); from += BATCH) {
            List<Input> batch = missing.subList(from, Math.min(missing.size(), from + BATCH));
            long start = System.nanoTime();
            List<float[]> out = client.embed(batch);
            callMillis.add((System.nanoTime() - start) / 1_000_000);
            for (int i = 0; i < batch.size(); i++) {
                vectors.put(key(batch.get(i)), out.get(i));
            }
            dirty = true;
        }
        return inputs.stream().map(i -> vectors.get(key(i))).toList();
    }

    /** Thời gian từng lời gọi Gemini trong lần chạy này (để tính p50, 4.6). */
    public List<Long> callMillis() {
        return callMillis;
    }

    public void saveIfChanged(ObjectMapper json) {
        if (!dirty) {
            return;
        }
        try {
            json.writeValue(file.toFile(), vectors);
            dirty = false;
        } catch (IOException e) {
            throw new UncheckedIOException("Không ghi được " + file, e);
        }
    }

    private static String sha256(byte[] b) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
