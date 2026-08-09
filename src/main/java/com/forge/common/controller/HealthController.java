package com.forge.common.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (one == null || one != 1) {
                return ResponseEntity.status(503).body(Map.of(
                        "status", "DEGRADED",
                        "db", "DOWN",
                        "timestamp", Instant.now().toString()
                ));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DEGRADED",
                    "db", "DOWN",
                    "error", ex.getClass().getSimpleName(),
                    "timestamp", Instant.now().toString()
            ));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("db", "UP");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }
}
