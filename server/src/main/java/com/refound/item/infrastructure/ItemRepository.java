package com.refound.item.infrastructure;

import static com.refound.common.Times.utc;

import com.refound.common.GeoPoint;
import com.refound.item.domain.Item;
import com.refound.item.domain.ItemImage;
import com.refound.item.domain.ItemStatus;
import com.refound.item.domain.ItemType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Bảng {@code items}, {@code item_images}, {@code verification_secrets} (02 §2.4). Tọa độ ghi bằng
 * {@code st_makepoint(lng, lat)}: kinh độ trước, vĩ độ sau.
 */
@Repository
public class ItemRepository {

    /** Cột của {@link Item}; bảng {@code items} đặt bí danh {@code i}, {@code categories} là {@code c}. */
    static final String COLUMNS = """
            i.id, i.author_id, i.type, i.status, i.category, c.sensitive, i.description, i.happened_at,
            st_y(i.location::geometry) as lat, st_x(i.location::geometry) as lng,
            st_y(i.public_location::geometry) as public_lat, st_x(i.public_location::geometry) as public_lng,
            i.radius_m, i.embedding_status, i.closed_reason, i.created_at, i.updated_at, i.last_activity_at, i.deleted_at
            """;
    static final String FROM = " from refound.items i join refound.categories c on c.code = i.category ";

    private final JdbcTemplate jdbc;

    public ItemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Item> findById(UUID id) {
        return jdbc.query("select " + COLUMNS + FROM + "where i.id = ?", ItemRepository::map, id).stream().findFirst();
    }

    /** Ảnh của nhiều tin trong một truy vấn, theo tin rồi theo vị trí. */
    public List<ItemImage> findImages(Collection<UUID> itemIds) {
        if (itemIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(
                "select id, item_id, position, full_path, thumb_path from refound.item_images "
                        + "where item_id = any(?) order by item_id, position",
                ps -> ps.setArray(1, ps.getConnection().createArrayOf("uuid", itemIds.toArray())),
                (rs, i) -> new ItemImage(rs.getObject("id", UUID.class), rs.getObject("item_id", UUID.class),
                        rs.getInt("position"), rs.getString("full_path"), rs.getString("thumb_path")));
    }

    /**
     * Khóa dòng {@code users} của người đăng tới hết transaction, để hai request đăng tin song song của cùng một
     * người đếm BR-09 lần lượt chứ không cùng thấy 9 tin rồi cùng ghi.
     */
    public void lockAuthor(UUID authorId) {
        jdbc.query("select 1 from refound.users where id = ? for update", rs -> {}, authorId);
    }

    /** Thời điểm tạo các tin (kể cả đã xóa) của một người sau {@code since}. */
    public List<Instant> createdSince(UUID authorId, Instant since) {
        return jdbc.query("select created_at from refound.items where author_id = ? and created_at > ?",
                (rs, i) -> rs.getObject("created_at", OffsetDateTime.class).toInstant(), authorId, utc(since));
    }

    /** Ghi tin mới với {@code embedding_status = pending}, {@code next_embed_at = now} để job làm ngay (I1). */
    public void insert(Item item) {
        jdbc.update("""
                insert into refound.items (id, author_id, type, status, category, description, happened_at,
                  location, public_location, radius_m, next_embed_at, last_activity_at, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?,
                  st_setsrid(st_makepoint(?, ?), 4326)::geography, st_setsrid(st_makepoint(?, ?), 4326)::geography,
                  ?, ?, ?, ?, ?)
                """,
                item.id(), item.authorId(), item.type().db(), item.status().db(), item.category(), item.description(),
                utc(item.happenedAt()),
                item.location().lng(), item.location().lat(),
                item.publicLocation().lng(), item.publicLocation().lat(),
                item.radiusM(), utc(item.createdAt()), utc(item.lastActivityAt()), utc(item.createdAt()),
                utc(item.updatedAt()));
    }

    public void insertImage(ItemImage image) {
        jdbc.update("insert into refound.item_images (id, item_id, position, full_path, thumb_path) values (?, ?, ?, ?, ?)",
                image.id(), image.itemId(), image.position(), image.fullPath(), image.thumbPath());
    }

    public void upsertSecret(UUID itemId, String question, String answer) {
        jdbc.update("""
                insert into refound.verification_secrets (item_id, question, answer) values (?, ?, ?)
                on conflict (item_id) do update set question = excluded.question, answer = excluded.answer
                """, itemId, question, answer);
    }

    static Item map(ResultSet rs, int row) throws SQLException {
        return new Item(
                rs.getObject("id", UUID.class),
                rs.getObject("author_id", UUID.class),
                ItemType.fromDb(rs.getString("type")),
                ItemStatus.fromDb(rs.getString("status")),
                rs.getString("category"),
                rs.getBoolean("sensitive"),
                rs.getString("description"),
                instant(rs, "happened_at"),
                new GeoPoint(rs.getDouble("lat"), rs.getDouble("lng")),
                new GeoPoint(rs.getDouble("public_lat"), rs.getDouble("public_lng")),
                (Integer) rs.getObject("radius_m"),
                rs.getString("embedding_status"),
                rs.getString("closed_reason"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"),
                instant(rs, "last_activity_at"),
                instant(rs, "deleted_at"));
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime t = rs.getObject(column, OffsetDateTime.class);
        return t == null ? null : t.toInstant();
    }
}
