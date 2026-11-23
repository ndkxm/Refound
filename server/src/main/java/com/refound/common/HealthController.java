package com.refound.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Render health check gọi không tham số (không chạm DB). Cron hằng ngày gọi {@code ?db=true}
 * để chạm Supabase, tránh project bị tạm dừng (docs/kien-truc/07 §7.5).
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health(@RequestParam(defaultValue = "false") boolean db) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "up");
        if (!db) {
            return ResponseEntity.ok(body);
        }
        boolean dbUp;
        try {
            dbUp = Integer.valueOf(1).equals(jdbc.queryForObject("select 1", Integer.class));
        } catch (RuntimeException e) {
            dbUp = false;
        }
        body.put("db", dbUp ? "up" : "down");
        return ResponseEntity.status(dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
