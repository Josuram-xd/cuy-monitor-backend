package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.ColorAlreadyUsedException;
import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.CoatColor;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigBreed;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.port.in.GetCageHealthUseCase;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CageController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class CageControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GetCageHealthUseCase getCageHealthUseCase;
    @MockitoBean
    private ListGuineaPigsUseCase listGuineaPigsUseCase;
    @MockitoBean
    private RegisterGuineaPigUseCase registerGuineaPigUseCase;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void everyEndpointNeedsASession() throws Exception {
        mvc.perform(get("/api/v1/cages/cage-1/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Canela\",\"markColor\":\"RED\"}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(listGuineaPigsUseCase, registerGuineaPigUseCase, getCageHealthUseCase);
    }

    @Test
    void listsTheGuineaPigsOfTheCage() throws Exception {
        given(listGuineaPigsUseCase.list("cage-1")).willReturn(List.of(pig(1, "Canela", MarkColor.RED)));

        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Canela"))
                .andExpect(jsonPath("$[0].markColor").value("RED"))
                .andExpect(jsonPath("$[0].status").value("NORMAL"))
                .andExpect(jsonPath("$[0].statusSince").value("2026-10-05T14:32:00Z"));
    }

    @Test
    void registersAGuineaPigAndAnswers201() throws Exception {
        given(registerGuineaPigUseCase.register(argThat(c -> "cage-1".equals(c.cageCode())
                && "Canela".equals(c.name()) && c.markColor() == MarkColor.RED)))
                .willReturn(pig(7, "Canela", MarkColor.RED));

        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Canela\",\"markColor\":\"RED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("NORMAL"));
    }

    @Test
    void registersAGuineaPigWithItsProfile() throws Exception {
        GuineaPigProfile profile = new GuineaPigProfile(GuineaPigBreed.TEDDY, CoatColor.CREAM, 850, "tranquila");
        given(registerGuineaPigUseCase.register(argThat(c -> "Canela".equals(c.name()) && profile.equals(c.profile()))))
                .willReturn(GuineaPig.restore(7L, "cage-1", "Canela", MarkColor.RED, HealthStatus.NORMAL, NOW, true,
                        NOW, profile));

        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Canela\",\"markColor\":\"RED\",\"breed\":\"TEDDY\","
                                + "\"coatColor\":\"CREAM\",\"initialWeightGrams\":850,\"notes\":\"  tranquila \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.breed").value("TEDDY"))
                .andExpect(jsonPath("$.coatColor").value("CREAM"))
                .andExpect(jsonPath("$.initialWeightGrams").value(850))
                .andExpect(jsonPath("$.notes").value("tranquila"));
    }

    @Test
    void theProfileIsNullWhenTheGuineaPigHasNone() throws Exception {
        given(listGuineaPigsUseCase.list("cage-1")).willReturn(List.of(pig(1, "Canela", MarkColor.RED)));

        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs").with(token()))
                .andExpect(jsonPath("$[0].breed").value(nullValue()))
                .andExpect(jsonPath("$[0].notes").value(nullValue()));
    }

    @Test
    void rejectsAnInvalidProfileWith400() throws Exception {
        String base = "{\"name\":\"Canela\",\"markColor\":\"RED\",";
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(base + "\"breed\":\"DRAGON\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(base + "\"coatColor\":\"PINK\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(base + "\"initialWeightGrams\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.initialWeightGrams").exists());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(base + "\"initialWeightGrams\":5000}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(base + "\"notes\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.notes").exists());
        verifyNoInteractions(registerGuineaPigUseCase);
    }

    @Test
    void rejectsABlankNameOrAnUnknownColorWith400() throws Exception {
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"markColor\":\"RED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.fields.name").exists());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Canela\",\"markColor\":\"PINK\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Canela\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(registerGuineaPigUseCase);
    }

    @Test
    void answers409WhenTheColorIsTaken() throws Exception {
        given(registerGuineaPigUseCase.register(any(RegisterGuineaPigCommand.class)))
                .willThrow(new ColorAlreadyUsedException());

        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pelusa\",\"markColor\":\"RED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conflict"));
    }

    @Test
    void answers404ForAnUnknownCage() throws Exception {
        given(listGuineaPigsUseCase.list("nope")).willThrow(new CageNotFoundException());
        given(getCageHealthUseCase.getHealth("nope")).willThrow(new CageNotFoundException());

        mvc.perform(get("/api/v1/cages/nope/guinea-pigs").with(token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
        mvc.perform(get("/api/v1/cages/nope/health").with(token())).andExpect(status().isNotFound());
    }

    @Test
    void returnsTheCageHealthInTheContractShape() throws Exception {
        GuineaPig canela = GuineaPig.restore(1L, "cage-1", "Canela", MarkColor.RED, HealthStatus.OBSERVED,
                NOW.minusSeconds(600), true, NOW.minusSeconds(3600));
        given(getCageHealthUseCase.getHealth("cage-1")).willReturn(new CageHealthSummary("cage-1",
                HealthStatus.OBSERVED, List.of(canela),
                new CageHealthSummary.Audio(HealthStatus.NORMAL, NOW.minusSeconds(120)),
                new CageHealthSummary.Weight(HealthStatus.NORMAL, 812.4, NOW.minusSeconds(60)), NOW));

        mvc.perform(get("/api/v1/cages/cage-1/health").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cageId").value("cage-1"))
                .andExpect(jsonPath("$.status").value("OBSERVED"))
                .andExpect(jsonPath("$.guineaPigs[0].name").value("Canela"))
                .andExpect(jsonPath("$.guineaPigs[0].status").value("OBSERVED"))
                .andExpect(jsonPath("$.audio.status").value("NORMAL"))
                .andExpect(jsonPath("$.audio.lastEventAt").value("2026-10-05T14:30:00Z"))
                .andExpect(jsonPath("$.weight.lastGrams").value(812.4))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-05T14:32:00Z"));
    }

    private static GuineaPig pig(long id, String name, MarkColor color) {
        return GuineaPig.restore(id, "cage-1", name, color, HealthStatus.NORMAL, NOW, true, NOW);
    }

    private static RequestPostProcessor token() {
        return jwt().jwt(jwt -> jwt.subject("3f2a4b5c-0000-4000-8000-000000000001"));
    }
}
