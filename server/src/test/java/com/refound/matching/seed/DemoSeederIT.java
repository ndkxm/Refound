package com.refound.matching.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.common.storage.StorageClient;
import com.refound.item.infrastructure.CategoryRepository;
import com.refound.matching.domain.EmbeddingText;
import com.refound.matching.infrastructure.EmbeddingClient;
import com.refound.matching.infrastructure.EmbeddingClient.Input;
import com.refound.support.FakeStorage;
import com.refound.support.IntegrationTest;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** P0-13: {@code --seed} nạp bộ mẫu kèm vector có sẵn, chạy lại không nhân đôi. */
@IntegrationTest
class DemoSeederIT {

    @TempDir
    Path dir;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    FakeStorage storage;

    @Autowired
    CategoryRepository categories;

    @Autowired
    EmbeddingClient embeddings;

    @Autowired
    ObjectMapper json;

    @Test
    void seedsItemsWithCachedVectorsOnce() throws Exception {
        String f = "seed-f-" + UUID.randomUUID();
        String l = "seed-l-" + UUID.randomUUID();
        Files.createDirectories(dir.resolve("images"));
        Files.write(dir.resolve("images/f.jpg"), jpeg(1600, 1200));
        Files.writeString(dir.resolve(EvalDataset.FILE), """
                {"items": [
                  {"id": "%s", "type": "found", "category": "wallet", "description": "Ví da nâu gập đôi",
                   "happenedAt": "2026-11-20T09:00:00+07:00", "lat": 10.8701234, "lng": 106.8031234,
                   "images": ["f.jpg"], "question": "Trong ví có gì?", "answer": "Thẻ xe"},
                  {"id": "%s", "type": "lost", "category": "wallet", "description": "Mất ví nâu ở thư viện",
                   "happenedAt": "2026-11-20T08:00:00+07:00", "lat": 10.87, "lng": 106.80, "radiusM": 300,
                   "expectedFoundId": "%s"}
                ]}
                """.formatted(f, l, f), StandardCharsets.UTF_8);

        // Cache có đủ vector cho tin found; tin lost thiếu → phải để pending cho job embedding
        VectorCache cache = VectorCache.load(dir, embeddings.model(), json);
        cache.getOrEmbed(List.of(new Input.Text(EmbeddingText.forItem("Ví", "Ví da nâu gập đôi", null)),
                new Input.Image(JpegResize.maxSide(jpeg(1600, 1200), 512))), embeddings);
        cache.saveIfChanged(json);

        DemoSeeder seeder = new DemoSeeder(jdbc, tx, storage, categories, embeddings, json, dir.toString());
        seeder.run(new DefaultApplicationArguments("--seed"));

        UUID foundId = UUID.nameUUIDFromBytes(("refound-seed:" + f).getBytes(StandardCharsets.UTF_8));
        UUID lostId = UUID.nameUUIDFromBytes(("refound-seed:" + l).getBytes(StandardCharsets.UTF_8));
        assertThat(status(foundId)).isEqualTo("done");
        assertThat(status(lostId)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("select count(*) from refound.item_embeddings where item_id = ?",
                Integer.class, foundId)).isEqualTo(2); // chữ + một ảnh
        assertThat(jdbc.queryForObject("""
                select st_y(public_location::geometry) from refound.items where id = ?
                """, Double.class, foundId)).isEqualTo(10.870); // tin found hiện vị trí làm tròn (BR-11)
        assertThat(jdbc.queryForObject("select radius_m from refound.items where id = ?", Integer.class, lostId))
                .isEqualTo(300);
        BufferedImage full = ImageIO.read(new java.io.ByteArrayInputStream(storage.file("items/" + foundId + "/0.jpg")));
        BufferedImage thumb = ImageIO.read(
                new java.io.ByteArrayInputStream(storage.file("items/" + foundId + "/0_thumb.jpg")));
        assertThat(full.getWidth()).isEqualTo(1024);
        assertThat(thumb.getWidth()).isEqualTo(512);
        assertThat(thumb.getHeight()).isEqualTo(384);

        seeder.run(new DefaultApplicationArguments("--seed"));
        assertThat(jdbc.queryForObject("select count(*) from refound.items where id in (?, ?)", Integer.class,
                foundId, lostId)).isEqualTo(2);
    }

    @Test
    void doesNothingWithoutSeedOption() throws Exception {
        Files.writeString(dir.resolve(EvalDataset.FILE), "not json");
        new DemoSeeder(jdbc, tx, (StorageClient) storage, categories, embeddings, json, dir.toString())
                .run(new DefaultApplicationArguments());
    }

    private String status(UUID id) {
        return jdbc.queryForObject("select embedding_status from refound.items where id = ?", String.class, id);
    }

    private static byte[] jpeg(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(120, 72, 40));
        g.fillRect(0, 0, w, h);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }
}
