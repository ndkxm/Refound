package com.refound.matching.infrastructure;

import static com.refound.common.Times.utc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Hàng đợi embedding là các cột của {@code items} (ADR-01, 7.3). Job nhận việc bằng cách đẩy {@code next_embed_at}
 * ra sau (thuê việc), rồi gọi AI ngoài transaction, để không giữ kết nối DB (pool chỉ 3) trong lúc chờ AI. Server
 * chết giữa chừng thì hết hạn thuê, lượt vét sau làm lại. Kết quả chỉ ghi khi {@code updated_at} chưa đổi: tin bị
 * sửa hay xóa trong lúc chờ AI thì bỏ kết quả cũ (lần sửa đã đặt lại hàng đợi).
 */
@Repository
public class EmbeddingRepository {

    private final JdbcTemplate jdbc;

    public EmbeddingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Việc đã nhận: dữ liệu cần để tạo chữ và đọc ảnh. */
    public record Work(
            UUID itemId,
            Instant updatedAt,
            int attempts,
            String categoryName,
            boolean sensitive,
            String description,
            String aiCaption,
            List<Thumb> thumbs) {}

    public record Thumb(UUID imageId, String path) {}

    /** Một vector để ghi; {@code imageId} null với vector chữ. */
    public record Vector(String source, UUID imageId, float[] values) {}

    /**
     * Nhận tối đa {@code limit} tin {@code open} đến hạn ({@code FOR UPDATE SKIP LOCKED}, 4.4) và thuê tới
     * {@code leaseUntil}. Tin đóng, hết hạn, đã xóa không cần vector nên không nhận; gia hạn lại thì vẫn còn
     * {@code pending}.
     */
    public List<Work> claimDue(Instant now, Instant leaseUntil, int limit) {
        List<UUID> ids = jdbc.query("""
                update refound.items set next_embed_at = ?
                where id in (select id from refound.items
                             where embedding_status = 'pending' and next_embed_at <= ? and status = 'open'
                               and deleted_at is null
                             order by next_embed_at limit ? for update skip locked)
                returning id
                """, (rs, i) -> rs.getObject("id", UUID.class), utc(leaseUntil), utc(now), limit);
        return ids.stream().map(this::load).toList();
    }

    private Work load(UUID id) {
        List<Thumb> thumbs = jdbc.query(
                "select id, thumb_path from refound.item_images where item_id = ? order by position",
                (rs, i) -> new Thumb(rs.getObject("id", UUID.class), rs.getString("thumb_path")), id);
        return jdbc.queryForObject("""
                select i.updated_at, i.embedding_attempts, c.name_vi, c.sensitive, i.description, i.ai_caption
                from refound.items i join refound.categories c on c.code = i.category where i.id = ?
                """, (rs, n) -> new Work(id, rs.getObject("updated_at", OffsetDateTime.class).toInstant(),
                        rs.getInt("embedding_attempts"), rs.getString("name_vi"), rs.getBoolean("sensitive"),
                        rs.getString("description"), rs.getString("ai_caption"), thumbs), id);
    }

    /**
     * Ghi {@code done} và thay toàn bộ vector cũ (I2: vector của nội dung cũ không còn đúng). Gọi trong transaction.
     *
     * @param newCaption mô tả ảnh vừa tạo, null để giữ cái đang có
     * @return false nếu tin đã đổi từ lúc nhận việc (kết quả bị bỏ)
     */
    public boolean saveDone(Work w, String newCaption, String model, List<Vector> vectors) {
        int n = jdbc.update("""
                update refound.items set embedding_status = 'done', ai_caption = coalesce(?, ai_caption)
                where id = ? and updated_at = ? and embedding_status = 'pending' and deleted_at is null
                """, newCaption, w.itemId(), utc(w.updatedAt()));
        if (n == 0) {
            return false;
        }
        jdbc.update("delete from refound.item_embeddings where item_id = ?", w.itemId());
        jdbc.batchUpdate("""
                insert into refound.item_embeddings (item_id, source, image_id, model, embedding)
                values (?, ?, ?, ?, cast(? as vector))
                """, vectors.stream().map(v -> new Object[] {w.itemId(), v.source(), v.imageId(), model, literal(v.values())})
                .toList());
        return true;
    }

    /** Lỗi: ghi số lần lỗi và lần thử sau, hoặc {@code failed}. Mô tả ảnh vừa tạo vẫn lưu để lần sau khỏi gọi Groq. */
    public void saveFailure(Work w, int attempts, boolean failed, Instant nextAt, String newCaption) {
        jdbc.update("""
                update refound.items set embedding_attempts = ?, embedding_status = ?, next_embed_at = ?,
                  ai_caption = coalesce(?, ai_caption)
                where id = ? and updated_at = ? and embedding_status = 'pending'
                """, attempts, failed ? "failed" : "pending", utc(nextAt), newCaption, w.itemId(), utc(w.updatedAt()));
    }

    /** Trả việc về hàng đợi không tính lần lỗi (AI đang tạm dừng vì 429, 7.3). */
    public void release(UUID itemId, Instant nextAt) {
        jdbc.update("update refound.items set next_embed_at = ? where id = ? and embedding_status = 'pending'",
                utc(nextAt), itemId);
    }

    /** Dạng chữ {@code [x,y,…]} mà pgvector nhận. */
    public static String literal(float[] v) {
        StringBuilder s = new StringBuilder(v.length * 12).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                s.append(',');
            }
            s.append(v[i]);
        }
        return s.append(']').toString();
    }
}
