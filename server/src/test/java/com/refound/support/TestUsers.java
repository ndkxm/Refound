package com.refound.support;

import com.refound.auth.application.JwtIssuer;
import java.util.UUID;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fixture người dùng (09 §9.2): tạo user thẳng trong DB và JWT của server, không qua Firebase.
 * Mỗi lần gọi tạo một user mới, nên các test không giẫm dữ liệu của nhau.
 */
@TestComponent
public class TestUsers {

    private final JdbcTemplate jdbc;
    private final JwtIssuer jwt;

    public TestUsers(JdbcTemplate jdbc, JwtIssuer jwt) {
        this.jdbc = jdbc;
        this.jwt = jwt;
    }

    public TestUser a() {
        return create("Người A", "user");
    }

    public TestUser b() {
        return create("Người B", "user");
    }

    public TestUser c() {
        return create("Người C", "user");
    }

    public TestUser admin() {
        return create("Quản trị", "admin");
    }

    public void setStatus(TestUser user, String status) {
        jdbc.update("update refound.users set status = ? where id = ?", status, user.id());
    }

    public void setRole(TestUser user, String role) {
        jdbc.update("update refound.users set role = ? where id = ?", role, user.id());
    }

    private TestUser create(String name, String role) {
        String uid = "test-" + UUID.randomUUID();
        UUID id = jdbc.queryForObject(
                "insert into refound.users (firebase_uid, display_name, role) values (?, ?, ?) returning id",
                UUID.class, uid, name, role);
        return new TestUser(id, uid, jwt.issue(id, role));
    }

    public record TestUser(UUID id, String firebaseUid, String token) {

        public String bearer() {
            return "Bearer " + token;
        }
    }
}
