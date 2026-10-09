package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.port.in.ProcessEventUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IngestionController.class,
        properties = {"app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!", "app.api-key=test-key"})
@Import({SecurityConfig.class, JwtConfig.class})
class IngestionControllerTest {

    private static final UUID EVENT_ID = UUID.fromString("3f1c2a5e-8f0b-4a53-9d0e-6b1c0a7e9a11");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProcessEventUseCase processEventUseCase;

    @Test
    void validEventGoesThroughTheAdapterAndIsAccepted() throws Exception {
        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(audio("cage-1", 1, "0.88")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        ArgumentCaptor<HealthEvent> processed = ArgumentCaptor.forClass(HealthEvent.class);
        verify(processEventUseCase).process(processed.capture());
        assertThat(processed.getValue().eventId()).isEqualTo(EVENT_ID);
        assertThat(processed.getValue().signal()).isEqualTo(new AudioSignal(AudioLabel.DISTRESS, 0.88, 960));
    }

    @Test
    void invalidPayloadReturns400WithTheReason() throws Exception {
        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(audio("cage-1", 1, "\"high\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.message").value("payload.probability must be a number"));
        verifyNoInteractions(processEventUseCase);
    }

    @Test
    void unsupportedSchemaVersionReturns400() throws Exception {
        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(audio("cage-1", 2, "0.88")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTypeReturns400() throws Exception {
        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(audio("cage-1", 1, "0.88").replace("\"AUDIO\"", "\"VIDEO\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCageReturns404() throws Exception {
        willThrow(new CageNotFoundException("cage-9")).given(processEventUseCase).process(any());

        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(audio("cage-9", 1, "0.88")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void wrongApiKeyNeverReachesTheCore() throws Exception {
        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(audio("cage-1", 1, "0.88")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(processEventUseCase);
    }

    private static String audio(String cageId, int schemaVersion, String probability) {
        return """
                {"eventId":"%s","type":"AUDIO","cageId":"%s","timestamp":"2026-10-05T14:32:00Z",
                 "source":"ai-service","schemaVersion":%d,
                 "payload":{"label":"DISTRESS","probability":%s,"durationMs":960}}
                """.formatted(EVENT_ID, cageId, schemaVersion, probability);
    }
}
