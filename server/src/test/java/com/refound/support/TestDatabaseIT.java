package com.refound.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** U9: image supabase/postgres chạy được trong Testcontainers, Flyway chạy, có vector + postgis. */
@IntegrationTest

class TestDatabaseIT {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayRanAndExtensionsAvailable() {
        assertThat(jdbc.queryForObject(
                        "select count(*) from refound.flyway_schema_history", Integer.class))
                .isNotNull();
        assertThat(jdbc.queryForList(
                        "select name from pg_available_extensions where name in ('vector','postgis')", String.class))
                .containsExactlyInAnyOrder("vector", "postgis");
    }
}
