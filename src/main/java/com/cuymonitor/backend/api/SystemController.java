package com.cuymonitor.backend.api;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Smoke-test endpoint for the deployment. Not part of the domain. */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final JdbcTemplate jdbcTemplate;

    public SystemController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Integer cages = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cage", Integer.class);
        return Map.of("status", "UP", "cages", cages);
    }
}
