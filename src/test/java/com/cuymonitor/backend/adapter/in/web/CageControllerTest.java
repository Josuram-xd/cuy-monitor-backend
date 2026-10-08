package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.health.composite.CageHealth;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightReading;
import com.cuymonitor.backend.domain.port.in.GetCageHealthUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CageController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class, CageControllerTest.FixedClock.class})
class CageControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:05Z");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GetCageHealthUseCase getCageHealthUseCase;

    @Test
    void cageHealthNeedsAToken() throws Exception {
        mvc.perform(get("/api/v1/cages/cage-1/health")).andExpect(status().isUnauthorized());
    }

    @Test
    void cageHealthFollowsTheContract() throws Exception {
        GuineaPig canela = GuineaPig.restore(1L, "cage-1", "Canela", MarkColor.RED, HealthStatus.OBSERVED, NOW,
                true, NOW);
        given(getCageHealthUseCase.getCageHealth("cage-1")).willReturn(CageHealth.of("cage-1", List.of(canela),
                Optional.empty(), Optional.of(new WeightReading(1L, "cage-1", 812.4, true, NOW)), NOW));

        mvc.perform(get("/api/v1/cages/cage-1/health").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cageId").value("cage-1"))
                .andExpect(jsonPath("$.status").value("OBSERVED"))
                .andExpect(jsonPath("$.guineaPigs[0].id").value(1))
                .andExpect(jsonPath("$.guineaPigs[0].name").value("Canela"))
                .andExpect(jsonPath("$.guineaPigs[0].markColor").value("RED"))
                .andExpect(jsonPath("$.guineaPigs[0].status").value("OBSERVED"))
                .andExpect(jsonPath("$.audio.status").value("NORMAL"))
                .andExpect(jsonPath("$.audio.lastEventAt").doesNotExist())
                .andExpect(jsonPath("$.weight.lastGrams").value(812.4))
                .andExpect(jsonPath("$.weight.lastMeasuredAt").value("2026-10-05T14:32:05Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-05T14:32:05Z"));
    }

    @Test
    void unknownCageReturns404() throws Exception {
        given(getCageHealthUseCase.getCageHealth("cage-9")).willThrow(new CageNotFoundException("cage-9"));

        mvc.perform(get("/api/v1/cages/cage-9/health").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }
}
