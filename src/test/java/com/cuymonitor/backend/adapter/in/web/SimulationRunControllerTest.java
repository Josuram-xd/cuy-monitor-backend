package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.exception.CageAccessDeniedException;
import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import com.cuymonitor.backend.domain.port.in.ControlSimulationUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimulationRunController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class SimulationRunControllerTest {

    private static final UUID ACCOUNT = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");
    private static final String URL = "/api/v1/cages/sim-0a1b2c3d/simulation";

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private CageOperations cages;
    @MockitoBean
    private ControlSimulationUseCase simulation;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void needsASession() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(delete(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(cages, simulation);
    }

    @Test
    void reportsTheRunAsTheAiServiceAnswersIt() throws Exception {
        given(simulation.status("sim-0a1b2c3d")).willReturn("{\"running\":true,\"framesSent\":12}");

        mvc.perform(get(URL).with(token()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("{\"running\":true,\"framesSent\":12}"));
        verify(cages).getCage(ACCOUNT, "sim-0a1b2c3d");
    }

    @Test
    void stopsTheRunOfTheOwnersCage() throws Exception {
        mvc.perform(delete(URL).with(token())).andExpect(status().isNoContent());

        verify(simulation).stop("sim-0a1b2c3d");
    }

    @Test
    void someoneElsesCageIsForbiddenAndNothingIsStopped() throws Exception {
        willThrow(new CageAccessDeniedException()).given(cages).getCage(ACCOUNT, "sim-0a1b2c3d");

        mvc.perform(get(URL).with(token())).andExpect(status().isForbidden());
        mvc.perform(delete(URL).with(token())).andExpect(status().isForbidden());
        verify(simulation, never()).stop(anyString());
        verify(simulation, never()).status(anyString());
    }

    @Test
    void anAiServiceThatDoesNotAnswerIsUnavailable() throws Exception {
        willThrow(SimulationUnavailableException.aiServiceDown(new RuntimeException("down")))
                .given(simulation).status("sim-0a1b2c3d");

        mvc.perform(get(URL).with(token())).andExpect(status().isServiceUnavailable());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor token() {
        return jwt().jwt(j -> j.subject(ACCOUNT.toString()));
    }
}
