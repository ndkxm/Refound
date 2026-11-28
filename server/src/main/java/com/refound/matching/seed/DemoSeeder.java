package com.refound.matching.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.common.GeoPoint;
import com.refound.common.storage.StorageClient;
import com.refound.item.domain.Category;
import com.refound.item.domain.ItemType;
import com.refound.item.domain.LocationRounding;
import com.refound.item.infrastructure.CategoryRepository;
import com.refound.matching.domain.EmbeddingText;
import com.refound.matching.infrastructure.EmbeddingClient;
import com.refound.matching.infrastructure.EmbeddingClient.Input;
import com.refound.matching.infrastructure.EmbeddingRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code ./mvnw spring-boot:run -Dspring-boot.run.arguments=--seed} (09 §9.3): nạp bộ mẫu {@code eval/dataset.json}
 * làm dữ liệu demo, gắn sẵn vector từ {@code cache-<model>.json} nên không gọi Gemini. Tin nào thiếu vector thì để
 * {@code pending} cho job embedding làm. Chạy lại an toàn: id tin suy ra từ id trong bộ mẫu, tin đã có thì bỏ qua.
 * Tác giả là hai tài khoản mẫu không đăng nhập được (firebase_uid {@code seed:*}).
 */
@Component
public class DemoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final StorageClient storage;
    private final CategoryRepository categories;
    private final EmbeddingClient embeddings;
    private final ObjectMapper json;
    private final Path dir;

    public DemoSeeder(JdbcTemplate jdbc, TransactionTemplate tx, StorageClient storage, CategoryRepository categories,
            EmbeddingClient embeddings, ObjectMapper json,
            @Value("${refound.seed.dir:src/test/resources/eval}") String dir) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.storage = storage;
        this.categories = categories;
        this.embeddings = embeddings;
        this.json = json;
        this.dir = Path.of(dir);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("seed")) {
            return;
        }
        if (!EvalDataset.exists(dir)) {
            log.warn("--seed: không thấy {}, bỏ qua", dir.resolve(EvalDataset.FILE).toAbsolutePath());
            return;
        }
        EvalDataset data = EvalDataset.load(dir, json);
        VectorCache cache = VectorCache.load(dir, embeddings.model(), json);
        UUID finder = seedUser("seed:finder", "Người nhặt (mẫu)");
        UUID owner = seedUser("seed:owner", "Người mất (mẫu)");
        int added = 0;
        int ready = 0;
        for (EvalDataset.Item item : data.items()) {
            Optional<Boolean> r = seed(data, cache, item, item.found() ? finder : owner);
            if (r.isPresent()) {
                added++;
                ready += r.get() ? 1 : 0;
            }
        }
        log.info("--seed: thêm {} tin ({} có sẵn vector, {} chờ job embedding), bỏ qua {} tin đã có",
                added, ready, added - ready, data.items().size() - added);
    }

    private UUID seedUser(String firebaseUid, String name) {
        return jdbc.queryForObject("""
                insert into refound.users (firebase_uid, display_name) values (?, ?)
                on conflict (firebase_uid) do update set display_name = excluded.display_name
                returning id
                """, UUID.class, firebaseUid, name);
    }

    /** @return rỗng nếu tin đã có; {@code true} nếu gắn được đủ vector */
    private Optional<Boolean> seed(EvalDataset data, VectorCache cache, EvalDataset.Item item, UUID author) {
        UUID id = UUID.nameUUIDFromBytes(("refound-seed:" + item.id()).getBytes(StandardCharsets.UTF_8));
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from refound.items where id = ?)", Boolean.class, id))) {
            return Optional.empty();
        }
        Category category = categories.find(item.category())
                .orElseThrow(() -> new IllegalArgumentException(item.id() + ": không có danh mục " + item.category()));
        ItemType type = ItemType.fromDb(item.type());
        // Ảnh nhạy cảm không đăng kèm tin lost và không gửi AI (BR-08); với found vẫn lưu ảnh nhưng không có vector ảnh.
        List<String> names = category.sensitive() && type == ItemType.LOST ? List.of() : item.images();

        List<byte[]> thumbs = new ArrayList<>();
        for (int pos = 0; pos < names.size(); pos++) {
            byte[] original = data.image(names.get(pos));
            byte[] thumb = JpegResize.maxSide(original, 512);
            thumbs.add(thumb);
            storage.upload("items/" + id + "/" + pos + ".jpg", JpegResize.maxSide(original, 1024), "image/jpeg");
            storage.upload("items/" + id + "/" + pos + "_thumb.jpg", thumb, "image/jpeg");
        }

        Optional<float[]> text = cache.get(new Input.Text(EmbeddingText.forItem(category.nameVi(), item.description(), null)));
        List<Optional<float[]>> imageVectors = category.sensitive()
                ? List.of()
                : thumbs.stream().map(t -> cache.get(new Input.Image(t))).toList();
        boolean ready = text.isPresent() && imageVectors.stream().allMatch(Optional::isPresent);

        GeoPoint exact = new GeoPoint(item.lat(), item.lng());
        GeoPoint pub = LocationRounding.publicLocation(type, exact);
        tx.executeWithoutResult(s -> {
            jdbc.update("""
                    insert into refound.items (id, author_id, type, category, description, happened_at, location,
                      public_location, radius_m, embedding_status)
                    values (?, ?, ?, ?, ?, ?, st_setsrid(st_makepoint(?, ?), 4326)::geography,
                      st_setsrid(st_makepoint(?, ?), 4326)::geography, ?, ?)
                    """, id, author, type.db(), category.code(), item.description(), item.happenedAt(),
                    exact.lng(), exact.lat(), pub.lng(), pub.lat(),
                    type == ItemType.LOST ? (item.radiusM() == null ? 200 : item.radiusM()) : null,
                    ready ? "done" : "pending");
            List<UUID> imageIds = new ArrayList<>();
            for (int pos = 0; pos < thumbs.size(); pos++) {
                imageIds.add(jdbc.queryForObject("""
                        insert into refound.item_images (item_id, position, full_path, thumb_path)
                        values (?, ?, ?, ?) returning id
                        """, UUID.class, id, pos, "items/" + id + "/" + pos + ".jpg",
                        "items/" + id + "/" + pos + "_thumb.jpg"));
            }
            if (type == ItemType.FOUND) {
                jdbc.update("insert into refound.verification_secrets (item_id, question, answer) values (?, ?, ?)",
                        id, Optional.ofNullable(item.question()).orElse("Món đồ có đặc điểm gì riêng?"),
                        Optional.ofNullable(item.answer()).orElse("(bộ mẫu không ghi)"));
            }
            if (ready) {
                String sql = """
                        insert into refound.item_embeddings (item_id, source, image_id, model, embedding)
                        values (?, ?, ?, ?, cast(? as vector))
                        """;
                jdbc.update(sql, id, "text", null, embeddings.model(), EmbeddingRepository.literal(text.get()));
                for (int i = 0; i < imageVectors.size(); i++) {
                    jdbc.update(sql, id, "image", imageIds.get(i), embeddings.model(),
                            EmbeddingRepository.literal(imageVectors.get(i).get()));
                }
            }
        });
        return Optional.of(ready);
    }
}
