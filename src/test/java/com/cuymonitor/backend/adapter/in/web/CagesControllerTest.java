package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.in.CreateCageUseCase;
import com.cuymonitor.backend.domain.port.in.DeleteCageUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageUseCase;
import com.cuymonitor.backend.domain.port.in.ListCagesUseCase;
import com.cuymonitor.backend.domain.port.in.UpdateCageUseCase;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CagesController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class CagesControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-02T14:00:00Z");
    private static final UUID ACCOUNT = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ListCagesUseCase listCagesUseCase;
    @MockitoBean
    private GetCageUseCase getCageUseCase;
    @MockitoBean
    private CreateCageUseCase createCageUseCase;
    @MockitoBean
    private UpdateCageUseCase updateCageUseCase;
    @MockitoBean
    private DeleteCageUseCase deleteCageUseCase;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void everyEndpointNeedsASession() throws Exception {
        mvc.perform(get("/api/v1/cages")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/cages").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/cages/cage-1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(listCagesUseCase, createCageUseCase, deleteCageUseCase);
    }

    @Test
    void listsTheCagesOfTheAccountInTheToken() throws Exception {
        given(listCagesUseCase.list(ACCOUNT)).willReturn(List.of(cage("cage-1", "Jaula piloto", "Galpón")));

        mvc.perform(get("/api/v1/cages").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cageId").value("cage-1"))
                .andExpect(jsonPath("$[0].name").value("Jaula piloto"))
                .andExpect(jsonPath("$[0].location").value("Galpón"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-11-02T14:00:00Z"));
    }

    @Test
    void createsACageOwnedByTheAccountInTheToken() throws Exception {
        given(createCageUseCase.create(argThat(c -> ACCOUNT.equals(c.ownerId()) && "Patio".equals(c.name()))))
                .willReturn(cage("cage-0a1b2c3d", "Patio", null));

        mvc.perform(post("/api/v1/cages").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Patio\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cageId").value("cage-0a1b2c3d"));
    }

    @Test
    void rejectsABlankOrTooLongName() throws Exception {
        mvc.perform(post("/api/v1/cages").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/cages").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(createCageUseCase);
    }

    @Test
    void renamesACage() throws Exception {
        given(updateCageUseCase.update(any())).willReturn(cage("cage-1", "Nueva", "Patio"));

        mvc.perform(put("/api/v1/cages/cage-1").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nueva\",\"location\":\"Patio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nueva"));
    }

    @Test
    void deletesACage() throws Exception {
        mvc.perform(delete("/api/v1/cages/cage-1").with(token())).andExpect(status().isNoContent());

        verify(deleteCageUseCase).delete("cage-1");
    }

    @Test
    void anUnknownCageIs404() throws Exception {
        willThrow(new CageNotFoundException()).given(deleteCageUseCase).delete("cage-nope");

        mvc.perform(delete("/api/v1/cages/cage-nope").with(token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    private static Cage cage(String code, String name, String location) {
        return new Cage(1L, code, name, location, ACCOUNT, true, NOW);
    }

    private static RequestPostProcessor token() {
        return jwt().jwt(jwt -> jwt.subject(ACCOUNT.toString()));
    }
}
