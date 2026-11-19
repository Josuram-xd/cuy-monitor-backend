package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.domain.model.AccountActivity;
import com.cuymonitor.backend.domain.model.AiLogEntry;
import com.cuymonitor.backend.domain.port.out.AdminReportRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Plain SQL: these are aggregates over several tables, not entities, so JPA would only get in the way. */
@Component
public class AdminReportPersistenceAdapter implements AdminReportRepository {

    private static final String ACTIVITY = """
            SELECT c.owner_id,
                   count(DISTINCT c.id) AS cages,
                   (SELECT count(*) FROM guinea_pig g JOIN cage gc ON gc.id = g.cage_id
                     WHERE gc.owner_id = c.owner_id AND gc.active AND g.active) AS guinea_pigs,
                   (SELECT count(*) FROM alert a JOIN cage ac ON ac.id = a.cage_id
                     WHERE ac.owner_id = c.owner_id AND ac.active AND a.status = 'OPEN') AS open_alerts,
                   (SELECT max(e.received_at) FROM event e JOIN cage ec ON ec.id = e.cage_id
                     WHERE ec.owner_id = c.owner_id) AS last_activity
              FROM cage c
             WHERE c.active AND c.owner_id IS NOT NULL
             GROUP BY c.owner_id
            """;

    // the detail is written here, in Spanish, from the stored payload (the contract's camelCase fields)
    private static final String AI_LOG = """
            SELECT * FROM (
                (SELECT e.received_at AS at, c.code AS cage, e.type AS kind,
                        CASE e.type
                          WHEN 'BEHAVIOR' THEN concat(g.mark_color, ': quieto ', e.payload->>'stillSeconds',
                               ' s de ', e.payload->>'windowSeconds', ' s, comedero ', e.payload->>'feederVisits',
                               ', bebedero ', e.payload->>'watererVisits', ', anomalía ', e.payload->>'probAnomaly')
                          ELSE concat(e.payload->>'label', ' (probabilidad ', e.payload->>'probability', ')')
                        END AS detail
                   FROM event e
                   JOIN cage c ON c.id = e.cage_id
                   LEFT JOIN guinea_pig g ON g.id = e.guinea_pig_id
                  WHERE e.source = 'ai-service'
                  ORDER BY e.received_at DESC
                  LIMIT ?)
                UNION ALL
                (SELECT a.ai_note_at, c.code, 'NOTE', a.ai_note
                   FROM alert a
                   JOIN cage c ON c.id = a.cage_id
                  WHERE a.ai_note IS NOT NULL
                  ORDER BY a.ai_note_at DESC
                  LIMIT ?)
            ) log
            ORDER BY at DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbc;

    public AdminReportPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, AccountActivity> activityByAccount() {
        Map<UUID, AccountActivity> byAccount = new HashMap<>();
        jdbc.query(ACTIVITY, row -> {
            UUID owner = row.getObject("owner_id", UUID.class);
            byAccount.put(owner, new AccountActivity(owner, row.getInt("cages"), row.getInt("guinea_pigs"),
                    row.getInt("open_alerts"), instant(row.getTimestamp("last_activity"))));
        });
        return byAccount;
    }

    @Override
    public List<AiLogEntry> latestAiLog(int limit) {
        return jdbc.query(AI_LOG, (row, n) -> new AiLogEntry(instant(row.getTimestamp("at")), row.getString("cage"),
                AiLogEntry.Kind.valueOf(row.getString("kind")), row.getString("detail")), limit, limit, limit);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
