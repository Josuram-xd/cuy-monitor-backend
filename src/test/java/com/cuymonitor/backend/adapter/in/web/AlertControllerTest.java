package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.port.in.ListAlertsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AlertController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class AlertControllerTest {

    private static final Instant CREATED = Instant.parse("2026-10-05T14:20:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ListAlertsUseCase listAlertsUseCase;

    @Test
    void listsOpenAlertsWithTheContractShape() throws Exception {
        Alert alert = Alert.restore(7L, "cage-1", null, HealthStatus.ALERT, EventType.AUDIO,
                "Se escucharon chillidos de angustia en la jaula", AlertStatus.OPEN, CREATED, null);
        given(listAlertsUseCase.listAlerts(Optional.of(AlertStatus.OPEN))).willReturn(List.of(alert));

        mvc.perform(get("/api/v1/alerts").param("status", "OPEN").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].cageId").value("cage-1"))
                .andExpect(jsonPath("$[0].guineaPigId").doesNotExist())
                .andExpect(jsonPath("$[0].level").value("ALERT"))
                .andExpect(jsonPath("$[0].type").value("AUDIO"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-05T14:20:00Z"));
    }

    @Test
    void withoutStatusListsEveryAlert() throws Exception {
        given(listAlertsUseCase.listAlerts(Optional.empty())).willReturn(List.of());

        mvc.perform(get("/api/v1/alerts").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void unknownStatusReturns400() throws Exception {
        mvc.perform(get("/api/v1/alerts").param("status", "CLOSED").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid value for parameter status"));
        verifyNoInteractions(listAlertsUseCase);
    }

    @Test
    void alertsNeedAToken() throws Exception {
        mvc.perform(get("/api/v1/alerts")).andExpect(status().isUnauthorized());
    }
}
