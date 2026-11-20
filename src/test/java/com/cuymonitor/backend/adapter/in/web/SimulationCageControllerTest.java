package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.in.GetOrCreateSimulationCageUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimulationCageController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class SimulationCageControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final UUID ACCOUNT = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");
    private static final String URL = "/api/v1/simulation-cage";

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private GetOrCreateSimulationCageUseCase simulationCage;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void needsASession() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(simulationCage);
    }

    @Test
    void answersTheSimulationCageOfTheAccountInTheToken() throws Exception {
        given(simulationCage.getOrCreate(ACCOUNT))
                .willReturn(new Cage(7L, "sim-0a1b2c3d", "Simulación", null, ACCOUNT, true, NOW));

        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(ACCOUNT.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cageId").value("sim-0a1b2c3d"))
                .andExpect(jsonPath("$.simulated").value(true))
                .andExpect(jsonPath("$.monitored").value(false));
    }

    @Test
    void noClipIsUnavailable() throws Exception {
        willThrow(SimulationUnavailableException.noClip()).given(simulationCage).getOrCreate(ACCOUNT);

        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(ACCOUNT.toString()))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("service_unavailable"));
    }
}
