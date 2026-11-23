package com.refound.matching.infrastructure;

import static com.refound.common.Times.utc;

import com.refound.common.GeoPoint;
import com.refound.item.domain.ItemType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** SQL của tìm kiếm (04 §4.3): lọc cứng + max cosine theo tin, và cache vector câu truy vấn ({@code embedding_cache}). */
@Repository
public class SearchRepository {

    /** Lọc cứng 4.2: xa hơn 3 km (≈ phạm vi trường) thì không phải ứng viên. */
    public static final int MAX_DISTANCE_M = 3000;

    private final JdbcTemplate jdbc;

    public SearchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param vectors vector câu truy vấn (chữ và/hoặc ảnh); rỗng thì không tính cosine
     * @param happenedFrom, happenedTo lọc cứng theo thời gian (null là không lọc)
     * @param likePatterns tìm hạ cấp: tin phải chứa ít nhất một mẫu (rỗng là không lọc)
     */
    public record Query(
            ItemType type,
            List<float[]> vectors,
            String model,
            GeoPoint location,
            Instant happenedFrom,
            Instant happenedTo,
            List<String> likePatterns) {}

    /** {@code distanceM} đo tới vị trí công khai; {@code cosine} null nếu tin chưa có vector của model này. */
    public record Candidate(
            UUID id,
            String category,
            String groupCode,
            Instant happenedAt,
            Integer radiusM,
            String embeddingStatus,
            Double distanceM,
            Double cosine) {}

    /**
     * Tin {@code open}, chưa xóa, đúng loại, sắp theo cosine giảm dần. Khoảng cách tính tới {@code public_location}:
     * lý do "Cách khu vực mất … m" hiện cho người tìm, tính tới vị trí chính xác thì đổi vị trí tìm vài lần là dò ra
     * điểm nhặt (BR-11).
     */
    public List<Candidate> candidates(Query q, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select f.id, f.category, c.group_code, f.happened_at, f.radius_m, "
                + "f.embedding_status, ");
        if (q.location() != null) {
            sql.append("st_distance(f.public_location, st_setsrid(st_makepoint(?, ?), 4326)::geography) as dist_m, ");
            args.add(q.location().lng());
            args.add(q.location().lat());
        } else {
            sql.append("null::float8 as dist_m, ");
        }
        if (!q.vectors().isEmpty()) {
            sql.append("(select max(1 - (e.embedding <=> qv.v)) from refound.item_embeddings e cross join (values ");
            for (int i = 0; i < q.vectors().size(); i++) {
                sql.append(i == 0 ? "" : ", ").append("(cast(? as vector))");
                args.add(EmbeddingRepository.literal(q.vectors().get(i)));
            }
            sql.append(") as qv(v) where e.item_id = f.id and e.model = ?) as cos ");
            args.add(q.model());
        } else {
            sql.append("null::float8 as cos ");
        }
        sql.append("from refound.items f join refound.categories c on c.code = f.category "
                + "where f.type = ? and f.status = 'open' and f.deleted_at is null");
        args.add(q.type().db());
        if (q.happenedFrom() != null) {
            sql.append(" and f.happened_at >= ?");
            args.add(utc(q.happenedFrom()));
        }
        if (q.happenedTo() != null) {
            sql.append(" and f.happened_at <= ?");
            args.add(utc(q.happenedTo()));
        }
        if (q.location() != null) {
            sql.append(" and st_dwithin(f.public_location, st_setsrid(st_makepoint(?, ?), 4326)::geography, ?)");
            args.add(q.location().lng());
            args.add(q.location().lat());
            args.add(MAX_DISTANCE_M);
        }
        if (!q.likePatterns().isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < q.likePatterns().size(); i++) {
                sql.append(i == 0 ? "" : " or ").append("f.description ilike ? escape '\\'");
                args.add(q.likePatterns().get(i));
            }
            sql.append(")");
        }
        sql.append(" order by cos desc nulls last, f.happened_at desc, f.id limit ?");
        args.add(limit);
        return jdbc.query(sql.toString(), (rs, i) -> new Candidate(
                rs.getObject("id", UUID.class),
                rs.getString("category"),
                rs.getString("group_code"),
                rs.getObject("happened_at", OffsetDateTime.class).toInstant(),
                (Integer) rs.getObject("radius_m"),
                rs.getString("embedding_status"),
                (Double) rs.getObject("dist_m"),
                (Double) rs.getObject("cos")), args.toArray());
    }

    public Map<String, float[]> cached(Collection<String> keys) {
        Map<String, float[]> out = new HashMap<>();
        if (keys.isEmpty()) {
            return out;
        }
        jdbc.query("select key, embedding::text as v from refound.embedding_cache where key = any(?)",
                ps -> ps.setArray(1, ps.getConnection().createArrayOf("text", keys.toArray())),
                rs -> {
                    out.put(rs.getString("key"), parse(rs.getString("v")));
                });
        return out;
    }

    public void cache(String key, float[] vector) {
        jdbc.update("insert into refound.embedding_cache (key, embedding) values (?, cast(? as vector)) "
                + "on conflict (key) do nothing", key, EmbeddingRepository.literal(vector));
    }

    /** {@code [0.1,0.2,…]} → float[]. */
    static float[] parse(String text) {
        String[] parts = text.substring(1, text.length() - 1).split(",");
        float[] v = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            v[i] = Float.parseFloat(parts[i]);
        }
        return v;
    }
}
