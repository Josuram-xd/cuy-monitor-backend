package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.domain.model.AccountActivity;
import com.cuymonitor.backend.domain.model.AiLogEntry;
import com.cuymonitor.backend.integration.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// the SQL of the admin reports, against the real schema (the migrations of cuy-monitor-db)
class AdminReportPersistenceTest extends PostgresIntegrationTest {

    @Autowired
    private AdminReportPersistenceAdapter reports;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void summarisesEachAccountAndListsWhatTheAiDid() {
        UUID owner = UUID.randomUUID();
        String tag = owner.toString().substring(0, 8);
        jdbc.update("insert into app_user (id, username, full_name, email, password_hash, status, created_at,"
                + " updated_at) values (?, ?, 'Reportes', ?, 'hash', 'ACTIVE', now(), now())",
                owner, "rp" + tag, tag + "@report.local");
        long cage = jdbc.queryForObject("insert into cage (code, name, owner_id) values (?, 'Jaula', ?) returning id",
                Long.class, "cage-rp-" + tag, owner);
        jdbc.queryForObject("insert into cage (code, name, owner_id, active) values (?, 'Borrada', ?, false)"
                + " returning id", Long.class, "cage-rp-x" + tag, owner);
        long pig = jdbc.queryForObject("insert into guinea_pig (cage_id, mark_color) values (?, 'RED') returning id",
                Long.class, cage);
        Instant seen = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(60);
        jdbc.update("insert into event (id, type, cage_id, guinea_pig_id, source, occurred_at, received_at, payload)"
                + " values (?, 'BEHAVIOR', ?, ?, 'ai-service', ?, ?, ?::jsonb)", UUID.randomUUID(), cage, pig,
                Timestamp.from(seen), Timestamp.from(seen), """
                {"color":"RED","windowSeconds":60,"stillSeconds":55.0,"feederVisits":0,"watererVisits":1,
                 "avgGroupDistance":0.8,"probAnomaly":0.9,"detectionConfidence":0.9}""");
        jdbc.update("insert into alert (cage_id, guinea_pig_id, level, type, message, ai_note, ai_note_at)"
                + " values (?, ?, 'ALERT', 'BEHAVIOR', 'quieto', 'Revisa que coma.', ?)", cage, pig,
                Timestamp.from(seen.plusSeconds(30)));

        AccountActivity activity = reports.activityByAccount().get(owner);
        List<AiLogEntry> log = reports.latestAiLog(200).stream()
                .filter(entry -> entry.cageCode().equals("cage-rp-" + tag))
                .toList();

        // the switched-off cage is not counted
        assertThat(activity).isEqualTo(new AccountActivity(owner, 1, 1, 1, seen));
        assertThat(log).extracting(AiLogEntry::kind).containsExactly(AiLogEntry.Kind.NOTE, AiLogEntry.Kind.BEHAVIOR);
        assertThat(log.get(0).detail()).isEqualTo("Revisa que coma.");
        assertThat(log.get(1).detail()).startsWith("RED: quieto 55.0 s de 60 s, comedero 0");
    }
}
