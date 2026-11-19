package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.domain.port.out.SettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Table app_setting (V18). A handful of rows: replacing them all is simpler than working out a diff. */
@Component
public class SettingPersistenceAdapter implements SettingRepository {

    private final JdbcTemplate jdbc;

    public SettingPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, String> findAll() {
        Map<String, String> stored = new HashMap<>();
        jdbc.query("SELECT key, value FROM app_setting", row -> {
            stored.put(row.getString("key"), row.getString("value"));
        });
        return stored;
    }

    @Override
    @Transactional
    public void replaceAll(Map<String, String> changes, UUID changedBy) {
        jdbc.update("DELETE FROM app_setting");
        changes.forEach((key, value) -> jdbc.update(
                "INSERT INTO app_setting (key, value, updated_at, updated_by) VALUES (?, ?, now(), ?)",
                key, value, changedBy));
    }
}
