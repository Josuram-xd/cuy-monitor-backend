package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.in.ConnectHardwareStationUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HardwareStationController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class HardwareStationControllerTest {

    private static final UUID ACCOUNT = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private CageOperations cages;
    @MockitoBean
    private ConnectHardwareStationUseCase station;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void connectsTheKitToAFarmCage() throws Exception {
        given(cages.getCage(ACCOUNT, "cage-1")).willReturn(new Cage(1L, "cage-1", "Jaula piloto"));
        given(station.connect("cage-1")).willReturn(true);

        mvc.perform(put("/api/v1/cages/cage-1/equipment").with(token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void theKitIsNeverConnectedToASimulationCage() throws Exception {
        given(cages.getCage(ACCOUNT, "sim-0a1b2c3d")).willReturn(new Cage(2L, "sim-0a1b2c3d", "Simulación"));

        mvc.perform(put("/api/v1/cages/sim-0a1b2c3d/equipment").with(token()))
                .andExpect(status().isConflict());
        verify(station, never()).connect(anyString());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor token() {
        return jwt().jwt(j -> j.subject(ACCOUNT.toString()));
    }
}
