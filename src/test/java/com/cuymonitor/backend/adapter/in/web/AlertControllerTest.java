package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.exception.CageAccessDeniedException;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.AlertNotFoundException;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertHistoryFilter;
import com.cuymonitor.backend.domain.model.AlertHistoryPage;
import com.cuymonitor.backend.domain.model.Paging;
import com.cuymonitor.backend.domain.model.TimeRange;
import org.mockito.ArgumentCaptor;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AlertController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class, TestClockConfig.class})
class AlertControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:00Z");
    private static final UUID ACCOUNT = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CageOperations cageOperations;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void needsASession() throws Exception {
        mvc.perform(get("/api/v1/alerts")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/alerts/7").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REVIEWED\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cageOperations);
    }

    @Test
    void listsAlertsInTheContractShape() throws Exception {
        given(cageOperations.listAlerts(ACCOUNT, null, null)).willReturn(List.of(
                Alert.restore(7L, "cage-1", 1L, HealthStatus.ALERT, EventType.BEHAVIOR, "Canela is still",
                        AlertStatus.OPEN, NOW, null),
                Alert.restore(8L, "cage-1", null, HealthStatus.CRITICAL, EventType.AUDIO, "Distress",
                        AlertStatus.REVIEWED, NOW.minusSeconds(60), NOW)));

        mvc.perform(get("/api/v1/alerts").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].cageId").value("cage-1"))
                .andExpect(jsonPath("$[0].guineaPigId").value(1))
                .andExpect(jsonPath("$[0].level").value("ALERT"))
                .andExpect(jsonPath("$[0].type").value("BEHAVIOR"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-05T14:32:00Z"))
                .andExpect(jsonPath("$[0].reviewedAt").doesNotExist())
                .andExpect(jsonPath("$[1].guineaPigId").doesNotExist())
                .andExpect(jsonPath("$[1].reviewedAt").value("2026-10-05T14:32:00Z"));
    }

    @Test
    void filtersByStatus() throws Exception {
        given(cageOperations.listAlerts(ACCOUNT, null, AlertStatus.OPEN)).willReturn(List.of());

        mvc.perform(get("/api/v1/alerts").param("status", "OPEN").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void rejectsAStatusThatDoesNotExist() throws Exception {
        mvc.perform(get("/api/v1/alerts").param("status", "DONE").with(token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
        verifyNoInteractions(cageOperations);
    }

    @Test
    void marksAnAlertAsReviewed() throws Exception {
        given(cageOperations.reviewAlert(ACCOUNT, 7)).willReturn(
                Alert.restore(7L, "cage-1", 1L, HealthStatus.ALERT, EventType.BEHAVIOR, "Canela is still",
                        AlertStatus.REVIEWED, NOW.minusSeconds(300), NOW));

        mvc.perform(patch("/api/v1/alerts/7").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVIEWED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"))
                .andExpect(jsonPath("$.reviewedAt").value("2026-10-05T14:32:00Z"));
    }

    @Test
    void onlyReviewedIsAccepted() throws Exception {
        mvc.perform(patch("/api/v1/alerts/7").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
        mvc.perform(patch("/api/v1/alerts/7").with(token()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(cageOperations, never()).reviewAlert(any(), anyLong());
    }

    @Test
    void answers404ForAnUnknownAlert() throws Exception {
        given(cageOperations.reviewAlert(ACCOUNT, 999)).willThrow(new AlertNotFoundException());

        mvc.perform(patch("/api/v1/alerts/999").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVIEWED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void filtersByCage() throws Exception {
        given(cageOperations.listAlerts(ACCOUNT, "cage-1", AlertStatus.OPEN)).willReturn(List.of());

        mvc.perform(get("/api/v1/alerts").param("status", "OPEN").param("cageId", "cage-1").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void answers403ForAnAlertOfAnotherAccountsCage() throws Exception {
        given(cageOperations.reviewAlert(ACCOUNT, 5)).willThrow(new CageAccessDeniedException());

        mvc.perform(patch("/api/v1/alerts/5").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVIEWED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    private static RequestPostProcessor token() {
        return jwt().jwt(jwt -> jwt.subject(ACCOUNT.toString()));
    }

    @Test
    void theAlertHistoryIsOnePageInTheContractShape() throws Exception {
        var range = new TimeRange(TestClockConfig.NOW.minus(Duration.ofDays(7)), TestClockConfig.NOW);
        var alert = Alert.restore(7L, "cage-1", 1L, HealthStatus.CRITICAL, EventType.BEHAVIOR, "Canela is still",
                AlertStatus.REVIEWED, NOW, NOW.plusSeconds(600));
        given(cageOperations.getAlertHistory(any(), any()))
                .willReturn(new AlertHistoryPage(List.of(alert), new Paging(0, 1), 3, range));

        mvc.perform(get("/api/v1/alerts/history").param("size", "1").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].level").value("CRITICAL"))
                .andExpect(jsonPath("$.items[0].reviewedAt").value("2026-10-05T14:42:00Z"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.from").value("2026-10-28T12:00:00Z"))
                .andExpect(jsonPath("$.to").value("2026-11-04T12:00:00Z"));
    }

    @Test
    void theAlertHistoryFiltersReachTheProxyAsTheyCame() throws Exception {
        var range = TimeRange.of(null, null, TestClockConfig.NOW);
        given(cageOperations.getAlertHistory(any(), any())).willReturn(AlertHistoryPage.empty(Paging.of(2, 10), range));

        mvc.perform(get("/api/v1/alerts/history").with(token())
                        .param("cageId", "cage-1").param("guineaPigId", "4").param("level", "ALERT")
                        .param("type", "BEHAVIOR").param("status", "OPEN")
                        .param("from", "2026-11-01T00:00:00Z").param("to", "2026-11-03T00:00:00Z")
                        .param("page", "2").param("size", "10"))
                .andExpect(status().isOk());

        var filter = ArgumentCaptor.forClass(AlertHistoryFilter.class);
        verify(cageOperations).getAlertHistory(org.mockito.ArgumentMatchers.eq(ACCOUNT), filter.capture());
        assertThat(filter.getValue()).isEqualTo(new AlertHistoryFilter("cage-1", 4L, HealthStatus.ALERT,
                EventType.BEHAVIOR, AlertStatus.OPEN,
                new TimeRange(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-03T00:00:00Z")),
                new Paging(2, 10)));
    }

    @Test
    void aBadAlertHistoryRequestIsA400AndNeverReachesTheProxy() throws Exception {
        mvc.perform(get("/api/v1/alerts/history").param("size", "500").with(token()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/alerts/history").param("page", "-1").with(token()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/alerts/history").param("level", "NORMAL").with(token()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/alerts/history").param("level", "SICK").with(token()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/alerts/history").param("from", "2026-01-01T00:00:00Z").with(token()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cageOperations);
    }
}
