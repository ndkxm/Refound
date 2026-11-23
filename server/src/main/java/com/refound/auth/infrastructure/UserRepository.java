package com.refound.auth.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.refound.auth.domain.NotificationPrefs;
import com.refound.auth.domain.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Bảng {@code refound.users} (02 §2.2). Ghi tên schema đầy đủ vì kết nối test không có {@code currentSchema}. */
@Repository
public class UserRepository {

    private static final String COLUMNS = "id, firebase_uid, display_name, avatar_path, role, status, reputation, "
            + "notification_prefs::text as notification_prefs";

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final RowMapper<User> mapper;

    public UserRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
        this.mapper = this::map;
    }

    public Optional<User> findById(UUID id) {
        return jdbc.query("select " + COLUMNS + " from refound.users where id = ?", mapper, id).stream().findFirst();
    }

    public Optional<User> findByFirebaseUid(String uid) {
        return jdbc.query("select " + COLUMNS + " from refound.users where firebase_uid = ?", mapper, uid)
                .stream().findFirst();
    }

    /** Tạo user mới; rỗng nếu {@code firebase_uid} đã có (hai lần đăng nhập đầu chạy song song). */
    public Optional<User> insertIfAbsent(String firebaseUid, String displayName) {
        return jdbc.query(
                        "insert into refound.users (firebase_uid, display_name) values (?, ?) "
                                + "on conflict (firebase_uid) do nothing returning " + COLUMNS,
                        mapper, firebaseUid, displayName)
                .stream().findFirst();
    }

    /** Một truy vấn theo khóa chính cho mỗi request (ADR-05, 03 §3.3). */
    public Optional<StatusAndRole> findStatusAndRole(UUID id) {
        return jdbc.query("select status, role from refound.users where id = ?",
                        (rs, i) -> new StatusAndRole(rs.getString("status"), rs.getString("role")), id)
                .stream().findFirst();
    }

    public void updateDisplayName(UUID id, String displayName) {
        jdbc.update("update refound.users set display_name = ? where id = ?", displayName, id);
    }

    public void updateAvatarPath(UUID id, String avatarPath) {
        jdbc.update("update refound.users set avatar_path = ? where id = ?", avatarPath, id);
    }

    public record StatusAndRole(String status, String role) {}

    private User map(ResultSet rs, int row) throws SQLException {
        return new User(
                rs.getObject("id", UUID.class),
                rs.getString("firebase_uid"),
                rs.getString("display_name"),
                rs.getString("avatar_path"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getInt("reputation"),
                NotificationPrefs.from(parse(rs.getString("notification_prefs"))));
    }

    private Map<String, Object> parse(String prefs) {
        try {
            return json.readValue(prefs, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("notification_prefs không phải JSON object", e);
        }
    }
}
