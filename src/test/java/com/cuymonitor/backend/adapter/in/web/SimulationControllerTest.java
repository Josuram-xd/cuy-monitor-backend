package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.CageAccessDeniedException;
import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import com.cuymonitor.backend.domain.port.in.StartSimulationUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimulationController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class SimulationControllerTest {

    private static final UUID ACCOUNT = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");
    private static final String URL = "/api/v1/cages/cage-1/simulation";

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private StartSimulationUseCase startSimulation;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void needsASession() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(startSimulation);
    }

    @Test
    void startsTheDemoForTheAccountInTheToken() throws Exception {
        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(ACCOUNT.toString()))))
                .andExpect(status().isAccepted());

        verify(startSimulation).start(ACCOUNT, "cage-1");
    }

    @Test
    void aCageWithoutGuineaPigsIsABadRequest() throws Exception {
        willThrow(SimulationUnavailableException.noGuineaPigs()).given(startSimulation).start(ACCOUNT, "cage-1");

        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(ACCOUNT.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ApiError.BAD_REQUEST));
    }

    @Test
    void anAiServiceThatDoesNotAnswerIsUnavailable() throws Exception {
        willThrow(SimulationUnavailableException.aiServiceDown(new RuntimeException("down")))
                .given(startSimulation).start(ACCOUNT, "cage-1");

        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(ACCOUNT.toString()))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void someoneElsesCageIsForbidden() throws Exception {
        willThrow(new CageAccessDeniedException()).given(startSimulation).start(ACCOUNT, "cage-1");

        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(ACCOUNT.toString()))))
                .andExpect(status().isForbidden());
    }
}
