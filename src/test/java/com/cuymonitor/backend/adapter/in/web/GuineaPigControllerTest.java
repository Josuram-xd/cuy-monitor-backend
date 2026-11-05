package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.exception.CageAccessDeniedException;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.GuineaPigHistory;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.model.TimeRange;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GuineaPigController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class, TestClockConfig.class})
class GuineaPigControllerTest {

    private static final Instant NOW = TestClockConfig.NOW;
    private static final UUID ACCOUNT = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CageOperations cageOperations;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void needsASession() throws Exception {
        mvc.perform(get("/api/v1/guinea-pigs/1/history")).andExpect(status().isUnauthorized());
        verifyNoInteractions(cageOperations);
    }

    @Test
    void answersTheHistoryInTheContractShapeForTheLast24HoursByDefault() throws Exception {
        TimeRange day = TimeRange.of(null, null, NOW);
        given(cageOperations.getGuineaPigHistory(ACCOUNT, 1L, day)).willReturn(new GuineaPigHistory(1L, day,
                List.of(new StateTransition(4L, 1L, HealthStatus.NORMAL, HealthStatus.OBSERVED, "Anomaly",
                        NOW.minusSeconds(600))),
                List.of(new GuineaPigHistory.BehaviorWindow(NOW.minusSeconds(60), 48, 0, 1, 0.72)),
                new BaselineProfile(1L, 30, 1, 0.3, NOW)));

        mvc.perform(get("/api/v1/guinea-pigs/1/history").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guineaPigId").value(1))
                .andExpect(jsonPath("$.from").value("2026-11-03T12:00:00Z"))
                .andExpect(jsonPath("$.to").value("2026-11-04T12:00:00Z"))
                .andExpect(jsonPath("$.transitions[0].toStatus").value("OBSERVED"))
                .andExpect(jsonPath("$.windows[0].stillSeconds").value(48))
                .andExpect(jsonPath("$.windows[0].avgGroupDistance").value(0.72))
                .andExpect(jsonPath("$.baseline.avgStillSeconds").value(30));
    }

    @Test
    void aRangeThatEndsBeforeItStartsIs400() throws Exception {
        mvc.perform(get("/api/v1/guinea-pigs/1/history").with(token())
                        .param("from", "2026-11-04T10:00:00Z").param("to", "2026-11-04T09:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
        mvc.perform(get("/api/v1/guinea-pigs/1/history").with(token()).param("from", "yesterday"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cageOperations);
    }

    @Test
    void theHistoryOfAnotherAccountsGuineaPigIs403() throws Exception {
        given(cageOperations.getGuineaPigHistory(eq(ACCOUNT), eq(7L), any())).willThrow(new CageAccessDeniedException());

        mvc.perform(get("/api/v1/guinea-pigs/7/history").with(token())).andExpect(status().isForbidden());
    }

    private static RequestPostProcessor token() {
        return jwt().jwt(jwt -> jwt.subject(ACCOUNT.toString()));
    }
}
