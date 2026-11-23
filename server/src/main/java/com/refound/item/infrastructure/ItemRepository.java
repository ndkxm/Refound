package com.refound.item.infrastructure;

import static com.refound.common.Times.utc;

import com.refound.common.Cursor;
import com.refound.common.GeoPoint;
import com.refound.item.domain.Bbox;
import com.refound.item.domain.Item;
import com.refound.item.domain.ItemImage;
import com.refound.item.domain.ItemStatus;
import com.refound.item.domain.ItemType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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

    /** Bộ lọc của danh sách công khai; trường null là không lọc. */
    public record PublicFilter(ItemType type, String category, Instant from, Instant to, Bbox bbox) {}

    /**
     * {@code GET /items}: chỉ tin {@code open}, chưa xóa (FR-ITEM-03). Khung bản đồ lọc theo {@code public_location}
     * chứ không theo vị trí chính xác: thu nhỏ khung dần sẽ dò ra điểm thật của tin found (ghi chú P0-01, BR-11).
     * Sắp theo thời điểm xảy ra mới nhất; lấy {@code limit} dòng.
     */
    public List<Item> listPublic(PublicFilter f, Cursor after, int limit) {
        StringBuilder sql = new StringBuilder("select " + COLUMNS + FROM + "where i.status = 'open' and i.deleted_at is null");
        List<Object> args = new ArrayList<>();
        if (f.type() != null) {
            sql.append(" and i.type = ?");
            args.add(f.type().db());
        }
        if (f.category() != null) {
            sql.append(" and i.category = ?");
            args.add(f.category());
        }
        if (f.from() != null) {
            sql.append(" and i.happened_at >= ?");
            args.add(utc(f.from()));
        }
        if (f.to() != null) {
            sql.append(" and i.happened_at <= ?");
            args.add(utc(f.to()));
        }
        if (f.bbox() != null) {
            sql.append(" and st_y(i.public_location::geometry) between ? and ?"
                    + " and st_x(i.public_location::geometry) between ? and ?");
            args.addAll(List.of(f.bbox().minLat(), f.bbox().maxLat(), f.bbox().minLng(), f.bbox().maxLng()));
        }
        if (after != null) {
            sql.append(" and (i.happened_at, i.id) < (?, ?)");
            args.add(utc(after.at()));
            args.add(after.id());
        }
        sql.append(" order by i.happened_at desc, i.id desc limit ?");
        args.add(limit);
        return jdbc.query(sql.toString(), ItemRepository::map, args.toArray());
    }

    /** {@code GET /me/items}: mọi trạng thái của mình trừ tin đã xóa, mới tạo trước. */
    public List<Item> listByAuthor(UUID authorId, ItemStatus status, Cursor after, int limit) {
        StringBuilder sql = new StringBuilder("select " + COLUMNS + FROM + "where i.author_id = ? and i.deleted_at is null");
        List<Object> args = new ArrayList<>(List.of(authorId));
        if (status != null) {
            sql.append(" and i.status = ?");
            args.add(status.db());
        }
        if (after != null) {
            sql.append(" and (i.created_at, i.id) < (?, ?)");
            args.add(utc(after.at()));
            args.add(after.id());
        }
        sql.append(" order by i.created_at desc, i.id desc limit ?");
        args.add(limit);
        return jdbc.query(sql.toString(), ItemRepository::map, args.toArray());
    }

    public Optional<Secret> findSecret(UUID itemId) {
        return jdbc.query("select question, answer from refound.verification_secrets where item_id = ?",
                (rs, i) -> new Secret(rs.getString("question"), rs.getString("answer")), itemId).stream().findFirst();
    }

    public record Secret(String question, String answer) {}

    /**
     * I2: ghi nội dung mới, chỉ khi tin còn {@code open} (khóa lạc quan theo trạng thái, 05). Sửa là "hoạt động":
     * {@code last_activity_at = now}, xóa {@code expiry_warned_at} để lần sắp hết hạn sau còn được báo.
     *
     * @param reembed mô tả hoặc danh mục đổi: đặt lại hàng đợi embedding (vector cũ do service xóa cùng transaction)
     * @param clearCaption danh mục mới nhạy cảm: bỏ mô tả ảnh của AI (BR-08)
     * @return false nếu tin không còn {@code open} hoặc đã xóa
     */
    public boolean update(Item next, boolean reembed, boolean clearCaption, Instant now) {
        String embedding = reembed
                ? ", embedding_status = 'pending', embedding_attempts = 0, next_embed_at = ?"
                : "";
        List<Object> args = new ArrayList<>(List.of(next.description(), next.category(), utc(next.happenedAt()),
                next.location().lng(), next.location().lat(), next.publicLocation().lng(), next.publicLocation().lat()));
        args.add(next.radiusM());
        args.add(utc(now));
        args.add(utc(now));
        if (reembed) {
            args.add(utc(now));
        }
        args.add(next.id());
        return jdbc.update("""
                update refound.items set description = ?, category = ?, happened_at = ?,
                  location = st_setsrid(st_makepoint(?, ?), 4326)::geography,
                  public_location = st_setsrid(st_makepoint(?, ?), 4326)::geography,
                  radius_m = ?, updated_at = ?, last_activity_at = ?, expiry_warned_at = null"""
                + embedding + (clearCaption ? ", ai_caption = null" : "")
                + " where id = ? and status = 'open' and deleted_at is null", args.toArray()) == 1;
    }

    public void deleteEmbeddings(UUID itemId) {
        jdbc.update("delete from refound.item_embeddings where item_id = ?", itemId);
    }

    /**
     * Đổi trạng thái {@code from → to} nếu tin vẫn ở {@code from} và chưa xóa (05: {@code UPDATE ... WHERE status}).
     *
     * @param closedReason ghi vào {@code closed_reason} khi {@code to = closed}, null thì giữ nguyên
     * @param touchActivity gia hạn (I9) là hoạt động: đặt lại {@code last_activity_at}, {@code expiry_warned_at}
     * @return false nếu có người đổi trước
     */
    public boolean changeStatus(UUID id, ItemStatus from, ItemStatus to, String closedReason, boolean touchActivity,
            Instant now) {
        String activity = touchActivity ? ", last_activity_at = ?, expiry_warned_at = null" : "";
        List<Object> args = new ArrayList<>(List.of(to.db(), utc(now)));
        args.add(closedReason);
        if (touchActivity) {
            args.add(utc(now));
        }
        args.addAll(List.of(id, from.db()));
        return jdbc.update("update refound.items set status = ?, updated_at = ?, closed_reason = coalesce(?, closed_reason)"
                + activity + " where id = ? and status = ? and deleted_at is null", args.toArray()) == 1;
    }

    /** I13: xóa mềm, giữ bản ghi cho claim/bàn giao cũ. */
    public boolean softDelete(UUID id, ItemStatus from, Instant now) {
        return jdbc.update("update refound.items set deleted_at = ?, updated_at = ? "
                + "where id = ? and status = ? and deleted_at is null", utc(now), utc(now), id, from.db()) == 1;
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
