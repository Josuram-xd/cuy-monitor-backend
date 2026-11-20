package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.application.fake.Photos;
import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.ReferenceEtagConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.port.in.GetGuineaPigPhotoUseCase;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The ai-service's read of the references: key first, photos as base64, and an unchanged list is a 304.
@WebMvcTest(controllers = GuineaPigReferenceController.class, properties = {
        "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!",
        "app.api-key=test-key"
})
@Import({SecurityConfig.class, JwtConfig.class, ReferenceEtagConfig.class})
class GuineaPigReferenceControllerTest {

    private static final String URL = "/api/v1/ingestion/cages/cage-1/guinea-pigs";

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ListGuineaPigsUseCase listGuineaPigs;
    @MockitoBean
    private GetGuineaPigPhotoUseCase getGuineaPigPhoto;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    private static GuineaPig guineaPig(long id, String markColor, boolean withPhoto) {
        return GuineaPig.restore(id, "cage-1", withPhoto ? "image/png" : null, markColor, HealthStatus.NORMAL,
                Instant.parse("2026-11-01T00:00:00Z"), true, Instant.parse("2026-11-01T00:00:00Z"),
                new GuineaPigProfile("PERUVIAN", "BROWN", null, null));
    }

    @Test
    void withoutTheApiKeyIsA401AndTheCageIsNotLookedUp() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
        mvc.perform(get(URL).header("X-API-Key", "wrong")).andExpect(status().isUnauthorized());

        verify(listGuineaPigs, never()).list(anyString());
    }

    @Test
    void anUnknownCageIsA404() throws Exception {
        when(listGuineaPigs.list("nope")).thenThrow(new CageNotFoundException());

        mvc.perform(get("/api/v1/ingestion/cages/nope/guinea-pigs").header("X-API-Key", "test-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eachGuineaPigComesWithItsPhotoInBase64AndItsRegisteredColors() throws Exception {
        when(listGuineaPigs.list("cage-1")).thenReturn(List.of(guineaPig(1, "RED", true),
                guineaPig(2, "BLUE", false)));
        when(getGuineaPigPhoto.getPhoto(anyString(), anyLong())).thenReturn(Photos.png());

        mvc.perform(get(URL).header("X-API-Key", "test-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].markColor").value("RED"))
                .andExpect(jsonPath("$[0].coatColor").value("BROWN"))
                .andExpect(jsonPath("$[0].breed").value("PERUVIAN"))
                .andExpect(jsonPath("$[0].photoContentType").value("image/png"))
                .andExpect(jsonPath("$[0].photo").value("AQID"))
                .andExpect(jsonPath("$[1].markColor").value("BLUE"))
                .andExpect(jsonPath("$[1].photoContentType").doesNotExist())
                .andExpect(jsonPath("$[1].photo").doesNotExist());
    }

    @Test
    void anUnchangedListAnswersNotModified() throws Exception {
        when(listGuineaPigs.list("cage-1")).thenReturn(List.of(guineaPig(1, "RED", false)));

        String etag = mvc.perform(get(URL).header("X-API-Key", "test-key"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("ETag");

        mvc.perform(get(URL).header("X-API-Key", "test-key").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }
}
