package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.MarkColorAlreadyUsedException;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GuineaPigController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class GuineaPigControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:10:00Z");
    private static final GuineaPig CANELA =
            GuineaPig.restore(1L, "cage-1", "Canela", MarkColor.RED, HealthStatus.NORMAL, NOW, true, NOW);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ListGuineaPigsUseCase listGuineaPigsUseCase;
    @MockitoBean
    private RegisterGuineaPigUseCase registerGuineaPigUseCase;

    @Test
    void listsTheGuineaPigsOfTheCage() throws Exception {
        given(listGuineaPigsUseCase.listGuineaPigs("cage-1")).willReturn(List.of(CANELA));

        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Canela"))
                .andExpect(jsonPath("$[0].markColor").value("RED"))
                .andExpect(jsonPath("$[0].status").value("NORMAL"))
                .andExpect(jsonPath("$[0].statusSince").value("2026-10-05T14:10:00Z"));
    }

    @Test
    void registerReturns201WithTheNewGuineaPig() throws Exception {
        given(registerGuineaPigUseCase.register(new RegisterGuineaPigCommand("cage-1", "Canela", MarkColor.RED)))
                .willReturn(CANELA);

        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Canela\",\"markColor\":\"RED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("NORMAL"));
    }

    @Test
    void registerWithoutNameReturns400() throws Exception {
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"markColor\":\"RED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
        verifyNoInteractions(registerGuineaPigUseCase);
    }

    @Test
    void registerWithAnUnknownColorReturns400() throws Exception {
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Canela\",\"markColor\":\"PINK\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithAUsedColorReturns409() throws Exception {
        given(registerGuineaPigUseCase.register(any())).willThrow(new MarkColorAlreadyUsedException(MarkColor.RED));

        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Otra\",\"markColor\":\"RED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conflict"));
    }

    @Test
    void registerInAnUnknownCageReturns404() throws Exception {
        given(registerGuineaPigUseCase.register(any())).willThrow(new CageNotFoundException("cage-9"));

        mvc.perform(post("/api/v1/cages/cage-9/guinea-pigs").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Canela\",\"markColor\":\"RED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void everyRouteNeedsAToken() throws Exception {
        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Canela\",\"markColor\":\"RED\"}"))
                .andExpect(status().isUnauthorized());
    }
}
