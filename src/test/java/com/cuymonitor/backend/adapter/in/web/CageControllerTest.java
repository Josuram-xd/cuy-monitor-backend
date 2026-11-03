package com.cuymonitor.backend.adapter.in.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.willThrow;
import com.cuymonitor.backend.domain.port.in.DeleteGuineaPigUseCase;
import com.cuymonitor.backend.domain.exception.GuineaPigNotFoundException;
import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.ColorAlreadyUsedException;
import com.cuymonitor.backend.domain.exception.UnknownCatalogOptionException;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.application.fake.Photos;
import com.cuymonitor.backend.domain.port.in.GetCageHealthUseCase;
import com.cuymonitor.backend.domain.port.in.GetGuineaPigPhotoUseCase;
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
import java.util.Base64;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CageController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class CageControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:32:00Z");
    private static final GuineaPigPhoto PHOTO = Photos.png();
    private static final String PHOTO_BASE64 = Base64.getEncoder().encodeToString(PHOTO.content());
    private static final String BODY = body("\"markColor\":\"RED\"");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GetCageHealthUseCase getCageHealthUseCase;
    @MockitoBean
    private ListGuineaPigsUseCase listGuineaPigsUseCase;
    @MockitoBean
    private RegisterGuineaPigUseCase registerGuineaPigUseCase;
    @MockitoBean
    private DeleteGuineaPigUseCase deleteGuineaPigUseCase;
    @MockitoBean
    private GetGuineaPigPhotoUseCase getGuineaPigPhotoUseCase;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void everyEndpointNeedsASession() throws Exception {
        mvc.perform(get("/api/v1/cages/cage-1/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").contentType(MediaType.APPLICATION_JSON)
                .content(BODY)).andExpect(status().isUnauthorized());
        verifyNoInteractions(listGuineaPigsUseCase, registerGuineaPigUseCase, getCageHealthUseCase);
    }

    @Test
    void listsTheGuineaPigsOfTheCage() throws Exception {
        given(listGuineaPigsUseCase.list("cage-1")).willReturn(List.of(pig(1, "RED")));

        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].photoUrl").value("/api/v1/cages/cage-1/guinea-pigs/1/photo"))
                .andExpect(jsonPath("$[0].markColor").value("RED"))
                .andExpect(jsonPath("$[0].status").value("NORMAL"))
                .andExpect(jsonPath("$[0].statusSince").value("2026-10-05T14:32:00Z"));
    }

    @Test
    void registersAGuineaPigAndAnswers201() throws Exception {
        given(registerGuineaPigUseCase.register(argThat(c -> "cage-1".equals(c.cageCode())
                && "RED".equals(c.markColor()) && PHOTO.equals(c.photo()))))
                .willReturn(pig(7, "RED"));

        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.photoUrl").value("/api/v1/cages/cage-1/guinea-pigs/7/photo"))
                .andExpect(jsonPath("$.status").value("NORMAL"));
    }

    @Test
    void servesThePhotoOfAGuineaPig() throws Exception {
        given(getGuineaPigPhotoUseCase.getPhoto("cage-1", 7L)).willReturn(PHOTO);

        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs/7/photo").with(token()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PHOTO.content()))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void thePhotoNeedsASessionAndAnswers404WhenThereIsNone() throws Exception {
        willThrow(new GuineaPigNotFoundException()).given(getGuineaPigPhotoUseCase).getPhoto("cage-1", 99L);

        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs/7/photo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs/99/photo").with(token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void rejectsAPhotoThatIsNotAnImageOrIsTooBigWith400() throws Exception {
        // an SVG is an image the browser runs: not accepted
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(photoBody(PHOTO_BASE64, "image/svg+xml")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.photoContentType").exists());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(photoBody("A".repeat(420_004), "image/png")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.photo").exists());
        // base64 of the right shape, 3 bytes over the limit
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(photoBody("A".repeat(409_604), "image/png")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("photo must be at most 307200 bytes"));
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(photoBody("no base64 @@", "image/png")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("photo is not valid base64"));
        verifyNoInteractions(registerGuineaPigUseCase);
    }

    @Test
    void registersAGuineaPigWithItsProfile() throws Exception {
        GuineaPigProfile profile = new GuineaPigProfile("TEDDY", "CREAM", 850, "tranquila");
        given(registerGuineaPigUseCase.register(argThat(c -> profile.equals(c.profile()))))
                .willReturn(GuineaPig.restore(7L, "cage-1", "image/png", "RED", HealthStatus.NORMAL, NOW, true,
                        NOW, profile));

        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"markColor\":\"RED\",\"breed\":\"TEDDY\",\"coatColor\":\"CREAM\","
                                + "\"initialWeightGrams\":850,\"notes\":\"  tranquila \"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.breed").value("TEDDY"))
                .andExpect(jsonPath("$.coatColor").value("CREAM"))
                .andExpect(jsonPath("$.initialWeightGrams").value(850))
                .andExpect(jsonPath("$.notes").value("tranquila"));
    }

    @Test
    void theProfileIsNullWhenTheGuineaPigHasNone() throws Exception {
        given(listGuineaPigsUseCase.list("cage-1")).willReturn(List.of(pig(1, "RED")));

        mvc.perform(get("/api/v1/cages/cage-1/guinea-pigs").with(token()))
                .andExpect(jsonPath("$[0].breed").value(nullValue()))
                .andExpect(jsonPath("$[0].notes").value(nullValue()));
    }

    @Test
    void rejectsACodeThatIsNotInTheCatalogWith400() throws Exception {
        willThrow(new UnknownCatalogOptionException(CatalogKind.BREED, "DRAGON"))
                .given(registerGuineaPigUseCase).register(any());

        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"markColor\":\"RED\",\"breed\":\"DRAGON\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.message").value("unknown breed: DRAGON"));
    }

    @Test
    void rejectsACodeLongerThanTheColumnWith400() throws Exception {
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"markColor\":\"" + "A".repeat(21) + "\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.markColor").exists());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"markColor\":\"\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.markColor").exists());
        verifyNoInteractions(registerGuineaPigUseCase);
    }

    @Test
    void rejectsAnInvalidProfileWith400() throws Exception {
        String base = body("\"markColor\":\"RED\",").replace("}", "");
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
    void deletesAGuineaPigAndAnswers204() throws Exception {
        mvc.perform(delete("/api/v1/cages/cage-1/guinea-pigs/7").with(token()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(deleteGuineaPigUseCase).delete("cage-1", 7L);
    }

    @Test
    void answers404WhenTheGuineaPigOrTheCageIsUnknown() throws Exception {
        willThrow(new GuineaPigNotFoundException()).given(deleteGuineaPigUseCase).delete("cage-1", 99L);
        willThrow(new CageNotFoundException()).given(deleteGuineaPigUseCase).delete("nope", 1L);

        mvc.perform(delete("/api/v1/cages/cage-1/guinea-pigs/99").with(token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").value("guinea pig not found"));
        mvc.perform(delete("/api/v1/cages/nope/guinea-pigs/1").with(token())).andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnIdThatIsNotANumberWith400() throws Exception {
        mvc.perform(delete("/api/v1/cages/cage-1/guinea-pigs/abc").with(token())).andExpect(status().isBadRequest());
        verifyNoInteractions(deleteGuineaPigUseCase);
    }

    @Test
    void deletingNeedsASession() throws Exception {
        mvc.perform(delete("/api/v1/cages/cage-1/guinea-pigs/7")).andExpect(status().isUnauthorized());
        verifyNoInteractions(deleteGuineaPigUseCase);
    }

    @Test
    void rejectsAMissingPhotoOrColorWith400() throws Exception {
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoContentType\":\"image/png\",\"markColor\":\"RED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.fields.photo").exists());
        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"breed\":\"TEDDY\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.markColor").exists());
        verifyNoInteractions(registerGuineaPigUseCase);
    }

    @Test
    void answers409WhenTheColorIsTaken() throws Exception {
        given(registerGuineaPigUseCase.register(any(RegisterGuineaPigCommand.class)))
                .willThrow(new ColorAlreadyUsedException());

        mvc.perform(post("/api/v1/cages/cage-1/guinea-pigs").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
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
        GuineaPig canela = GuineaPig.restore(1L, "cage-1", "image/png", "RED", HealthStatus.OBSERVED,
                NOW.minusSeconds(600), true, NOW.minusSeconds(3600));
        given(getCageHealthUseCase.getHealth("cage-1")).willReturn(new CageHealthSummary("cage-1",
                HealthStatus.OBSERVED, List.of(canela),
                new CageHealthSummary.Audio(HealthStatus.NORMAL, NOW.minusSeconds(120)),
                new CageHealthSummary.Weight(HealthStatus.NORMAL, 812.4, NOW.minusSeconds(60)), NOW));

        mvc.perform(get("/api/v1/cages/cage-1/health").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cageId").value("cage-1"))
                .andExpect(jsonPath("$.status").value("OBSERVED"))
                .andExpect(jsonPath("$.guineaPigs[0].photoUrl")
                        .value("/api/v1/cages/cage-1/guinea-pigs/1/photo"))
                .andExpect(jsonPath("$.guineaPigs[0].status").value("OBSERVED"))
                .andExpect(jsonPath("$.audio.status").value("NORMAL"))
                .andExpect(jsonPath("$.audio.lastEventAt").value("2026-10-05T14:30:00Z"))
                .andExpect(jsonPath("$.weight.lastGrams").value(812.4))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-05T14:32:00Z"));
    }

    private static GuineaPig pig(long id, String color) {
        return GuineaPig.restore(id, "cage-1", "image/png", color, HealthStatus.NORMAL, NOW, true, NOW);
    }

    /** A register body with the photo already in it, plus whatever fields the test is about. */
    private static String body(String fields) {
        return "{\"photo\":\"" + PHOTO_BASE64 + "\",\"photoContentType\":\"image/png\"," + fields + "}";
    }

    private static String photoBody(String photo, String contentType) {
        return "{\"photo\":\"" + photo + "\",\"photoContentType\":\"" + contentType + "\",\"markColor\":\"RED\"}";
    }

    private static RequestPostProcessor token() {
        return jwt().jwt(jwt -> jwt.subject("3f2a4b5c-0000-4000-8000-000000000001"));
    }
}
