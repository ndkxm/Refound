package com.refound.matching.seed;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Bộ tin mẫu {@code eval/dataset.json} (04 §4.6): dùng cho cả đánh giá đối chiếu ({@code -Dgroups=eval}) lẫn nạp dữ liệu
 * demo ({@code --seed}). Ảnh nằm ở {@code eval/images/}. Định dạng: {@code eval/README.md}.
 */
public record EvalDataset(Path dir, List<Item> items) {

    public static final String FILE = "dataset.json";

    /**
     * @param expectedFoundId chỉ tin {@code lost}: id tin {@code found} đúng
     * @param images tên file trong {@code images/}, theo thứ tự vị trí 0–3
     */
    public record Item(String id, String type, String category, String description, OffsetDateTime happenedAt,
            double lat, double lng, Integer radiusM, List<String> images, String question, String answer,
            String expectedFoundId) {

        public boolean found() {
            return "found".equals(type);
        }

        public List<String> images() {
            return images == null ? List.of() : images;
        }
    }

    record File(List<Item> items) {}

    public static boolean exists(Path dir) {
        return Files.isRegularFile(dir.resolve(FILE));
    }

    public static EvalDataset load(Path dir, ObjectMapper json) {
        try {
            File f = json.copy()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true) // gõ sai tên trường thì báo ngay
                    .readValue(dir.resolve(FILE).toFile(), File.class);
            return new EvalDataset(dir, f.items());
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được " + dir.resolve(FILE), e);
        }
    }

    public byte[] image(String name) {
        try {
            return Files.readAllBytes(dir.resolve("images").resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException("Thiếu ảnh " + name, e);
        }
    }
}
