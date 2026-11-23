package com.refound.support;

import com.refound.support.TestUsers.TestUser;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fixture tin: ghi thẳng DB (không qua API) để dựng nhanh mọi trạng thái. Mặc định mỗi tin ở một tọa độ ngẫu nhiên
 * riêng, nên test danh sách lọc bằng {@code bbox} quanh tọa độ của mình không thấy tin của test khác.
 */
@TestComponent
public class TestItems {

    public static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    private final JdbcTemplate jdbc;
    private final FakeStorage storage;

    public TestItems(JdbcTemplate jdbc, FakeStorage storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    public Builder found(TestUser author) {
        return new Builder(author.id(), "found");
    }

    public Builder lost(TestUser author) {
        return new Builder(author.id(), "lost");
    }

    /** Một tọa độ ngẫu nhiên xa campus (vĩ độ −60..60), để test danh sách tự cô lập bằng bbox. */
    public static double[] randomSpot() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new double[] {Math.round(r.nextDouble(-60, 60) * 1000) / 1000.0, Math.round(r.nextDouble(-170, 170) * 1000) / 1000.0};
    }

    public final class Builder {

        private final UUID author;
        private final String type;
        private String category = "wallet";
        private String status = "open";
        private String description = "Ví da nâu";
        private double lat;
        private double lng;
        private int images;
        private boolean deleted;
        private Instant happenedAt = Instant.now().minus(1, ChronoUnit.HOURS);
        private Instant createdAt = Instant.now();

        Builder(UUID author, String type) {
            this.author = author;
            this.type = type;
            double[] spot = randomSpot();
            this.lat = spot[0];
            this.lng = spot[1];
            this.images = "found".equals(type) ? 1 : 0;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /** Vị trí chính xác; vị trí công khai của tin found được làm tròn 3 chữ số như {@code LocationRounding}. */
        public Builder at(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
            return this;
        }

        public Builder images(int images) {
            this.images = images;
            return this;
        }

        public Builder deleted() {
            this.deleted = true;
            return this;
        }

        public Builder happenedAt(Instant happenedAt) {
            this.happenedAt = happenedAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UUID insert() {
            boolean found = "found".equals(type);
            double plat = found ? java.math.BigDecimal.valueOf(lat).setScale(3, java.math.RoundingMode.HALF_UP).doubleValue() : lat;
            double plng = found ? java.math.BigDecimal.valueOf(lng).setScale(3, java.math.RoundingMode.HALF_UP).doubleValue() : lng;
            OffsetDateTime created = createdAt.atOffset(ZoneOffset.UTC);
            UUID id = jdbc.queryForObject("""
                    insert into refound.items (author_id, type, status, category, description, happened_at, location,
                      public_location, radius_m, closed_reason, created_at, updated_at, last_activity_at, deleted_at)
                    values (?, ?, ?, ?, ?, ?, st_setsrid(st_makepoint(?, ?), 4326)::geography,
                      st_setsrid(st_makepoint(?, ?), 4326)::geography, ?, ?, ?, ?, ?, ?)
                    returning id
                    """, UUID.class, author, type, status, category, description,
                    happenedAt.atOffset(ZoneOffset.UTC), lng, lat, plng, plat, found ? null : 200,
                    "closed".equals(status) ? "Đã trả" : null, created, created, created,
                    deleted ? created : null);
            for (int pos = 0; pos < images; pos++) {
                String full = "items/" + id + "/" + pos + ".jpg";
                String thumb = "items/" + id + "/" + pos + "_thumb.jpg";
                storage.upload(full, JPEG, "image/jpeg");
                storage.upload(thumb, JPEG, "image/jpeg");
                jdbc.update("insert into refound.item_images (item_id, position, full_path, thumb_path) values (?, ?, ?, ?)",
                        id, pos, full, thumb);
            }
            if (found) {
                jdbc.update("insert into refound.verification_secrets (item_id, question, answer) values (?, ?, ?)",
                        id, "Trong ví có gì?", "Thẻ xe");
            }
            return id;
        }
    }
}
