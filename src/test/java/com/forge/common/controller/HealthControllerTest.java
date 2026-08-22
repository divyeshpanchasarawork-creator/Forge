package com.forge.common.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    private JdbcTemplate jdbcTemplate;
    private HealthController controller;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        controller = new HealthController(jdbcTemplate);
    }

    @Test
    void healthyDatabaseReportsUp() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);

        ResponseEntity<Map<String, Object>> result = controller.health();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("UP", result.getBody().get("status"));
        assertEquals("UP", result.getBody().get("db"));
        assertNotNull(result.getBody().get("timestamp"));
        assertEquals(3, result.getBody().size());
    }

    @Test
    void unexpectedQueryResultReportsDegraded() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(null);

        ResponseEntity<Map<String, Object>> result = controller.health();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
        assertEquals("DEGRADED", result.getBody().get("status"));
        assertEquals("DOWN", result.getBody().get("db"));
    }

    @Test
    void databaseFailureReportsDegraded() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        ResponseEntity<Map<String, Object>> result = controller.health();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, result.getStatusCode());
        assertEquals("DEGRADED", result.getBody().get("status"));
        assertEquals("DOWN", result.getBody().get("db"));
        assertNotNull(result.getBody().get("timestamp"));
        assertEquals(3, result.getBody().size());
    }
}
