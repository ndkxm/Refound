package com.refound.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import java.time.Duration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres giống Supabase (có sẵn pgvector + PostGIS) cho test *IT. Dùng: {@code @Import(TestDatabase.class)}.
 * Container dùng chung cho cả lượt chạy nhờ context cache của Spring. Xem docs/kien-truc/09-kiem-thu.md 9.2 (U9).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestDatabase {

    static final DockerImageName IMAGE =
            DockerImageName.parse("supabase/postgres:17.12.0.002").asCompatibleSubstituteFor("postgres");

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(IMAGE)
                .withDatabaseName("postgres")
                .withUsername("supabase_admin") // image Supabase khởi tạo hỏng nếu POSTGRES_USER khác supabase_admin
                .withPassword("postgres")
                // Giữ lệnh gốc của image (cấu hình Supabase ở /etc/postgresql); PostgreSQLContainer mặc định ghi đè nó.
                .withCommand("postgres", "-D", "/etc/postgresql")
                // Image Supabase chỉ in "ready to accept connections" một lần (lúc init); TCP chỉ mở khi server thật đã lên.
                .waitingFor(Wait.forSuccessfulCommand("pg_isready -h 127.0.0.1").withStartupTimeout(Duration.ofMinutes(3)));
    }
}
