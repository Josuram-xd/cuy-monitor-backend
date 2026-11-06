package com.cuymonitor.backend.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Task 14.3: the whole path of a sick guinea pig, through the real HTTP API and the real database.
// event over HTTP -> Chain of Responsibility -> State -> state_transition -> Observer -> alert -> alert history.
// Escalation times are zero here so the test does not wait half an hour; the rules are the production ones.
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.health.observed-to-alert=0s",
        "app.health.alert-to-critical=0s",
        "app.health.consecutive-anomaly-windows=3",
        "app.health.recovery-windows=3"
})
class HealthFlowIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID owner;
    private String cage;
    private long canela;
    private Instant clock;

    @BeforeEach
    void createCageWithAGuineaPig() {
        owner = UUID.randomUUID();
        String tag = owner.toString().substring(0, 8);
        jdbc.update("insert into app_user (id, username, full_name, email, password_hash, status, created_at,"
                        + " updated_at) values (?, ?, 'Integración', ?, 'hash', 'ACTIVE', now(), now())",
                owner, "it" + tag, tag + "@integration.local");
        cage = "cage-it-" + tag;
        long cageId = jdbc.queryForObject("insert into cage (code, name, owner_id) values (?, 'Jaula IT', ?)"
                + " returning id", Long.class, cage, owner);
        canela = jdbc.queryForObject("insert into guinea_pig (cage_id, mark_color) values (?, 'RED') returning id",
                Long.class, cageId);
        clock = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
    }

    private void sendWindow(boolean sick) throws Exception {
        sendWindow(sick, API_KEY, 202);
    }

    private void sendWindow(boolean sick, String apiKey, int expectedStatus) throws Exception {
        clock = clock.plusSeconds(10);
        String payload = sick
                ? """
                  {"color":"RED","windowSeconds":60,"stillSeconds":57,"feederVisits":0,"watererVisits":0,
                   "avgGroupDistance":0.85,"probAnomaly":0.9,"detectionConfidence":0.92}"""
                : """
                  {"color":"RED","windowSeconds":60,"stillSeconds":12,"feederVisits":2,"watererVisits":1,
                   "avgGroupDistance":0.25,"probAnomaly":0.05,"detectionConfidence":0.93}""";
        String event = """
                {"eventId":"%s","type":"BEHAVIOR","cageId":"%s","timestamp":"%s","source":"ai-service",
                 "schemaVersion":1,"payload":%s}""".formatted(UUID.randomUUID(), cage, clock, payload);
        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().is(expectedStatus));
    }

    private String statusOfCanela() {
        return jdbc.queryForObject("select current_status from guinea_pig where id = ?", String.class, canela);
    }

    private List<String> transitions() {
        return jdbc.queryForList("select from_status || '->' || to_status from state_transition"
                + " where guinea_pig_id = ? order by occurred_at, id", String.class, canela);
    }

    @Test
    void aSickGuineaPigClimbsToCriticalRaisesTwoAlertsAndRecoversWithoutANewOne() throws Exception {
        sendWindow(true);
        sendWindow(true);
        assertThat(statusOfCanela()).as("two anomalous windows are not enough").isEqualTo("NORMAL");

        sendWindow(true);
        assertThat(statusOfCanela()).isEqualTo("OBSERVED");
        sendWindow(true);
        assertThat(statusOfCanela()).isEqualTo("ALERT");
        sendWindow(true);
        assertThat(statusOfCanela()).isEqualTo("CRITICAL");

        for (int i = 0; i < 3; i++) {
            sendWindow(false);
        }
        assertThat(statusOfCanela()).isEqualTo("NORMAL");
        assertThat(transitions()).containsExactly(
                "NORMAL->OBSERVED", "OBSERVED->ALERT", "ALERT->CRITICAL", "CRITICAL->NORMAL");
        assertThat(jdbc.queryForList("select level from alert where guinea_pig_id = ? order by created_at",
                String.class, canela)).containsExactly("ALERT", "CRITICAL");

        mvc.perform(get("/api/v1/alerts/history").param("cageId", cage)
                        .with(jwt().jwt(token -> token.subject(owner.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].level").value("CRITICAL"))
                .andExpect(jsonPath("$.items[0].guineaPigId").value(canela))
                .andExpect(jsonPath("$.items[1].level").value("ALERT"));
    }

    @Test
    void anotherAccountCanNotReadTheHistoryOfTheCage() throws Exception {
        sendWindow(true);

        mvc.perform(get("/api/v1/alerts/history").param("cageId", cage)
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aWrongApiKeyIsRefusedAndChangesNothing() throws Exception {
        for (int i = 0; i < 4; i++) {
            sendWindow(true, "wrong-key", 401);
        }

        assertThat(statusOfCanela()).isEqualTo("NORMAL");
        assertThat(transitions()).isEmpty();
    }
}
