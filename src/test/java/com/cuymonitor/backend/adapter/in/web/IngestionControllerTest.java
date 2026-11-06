package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.ingestion.factory.AdapterFactorySelector;
import com.cuymonitor.backend.adapter.in.ingestion.factory.AudioAdapterFactory;
import com.cuymonitor.backend.adapter.in.ingestion.factory.CameraAdapterFactory;
import com.cuymonitor.backend.adapter.in.ingestion.factory.WeightAdapterFactory;
import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.port.in.ProcessEventUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Task 14.4: the ingestion endpoint with the real Factory Method + Adapter and a fake use case that only
// records what reached it, so each test shows exactly what the controller let through.
@WebMvcTest(controllers = IngestionController.class, properties = {
        "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!",
        "app.api-key=test-key"
})
@Import({SecurityConfig.class, JwtConfig.class, AdapterFactorySelector.class, CameraAdapterFactory.class,
        AudioAdapterFactory.class, WeightAdapterFactory.class, IngestionControllerTest.FakeUseCase.class})
class IngestionControllerTest {

    private static final String BEHAVIOR = """
            {"eventId":"9b2c1e7a-0000-4000-8000-000000000001","type":"BEHAVIOR","cageId":"cage-1",
             "timestamp":"2026-11-05T12:00:00Z","source":"ai-service","schemaVersion":1,
             "payload":{"color":"RED","windowSeconds":60,"stillSeconds":48,"feederVisits":0,"watererVisits":1,
                        "avgGroupDistance":0.72,"probAnomaly":0.81,"detectionConfidence":0.93}}""";
    private static final String AUDIO = """
            {"eventId":"9b2c1e7a-0000-4000-8000-000000000002","type":"AUDIO","cageId":"cage-1",
             "timestamp":"2026-11-05T12:00:00Z","source":"ai-service","schemaVersion":1,
             "payload":{"label":"DISTRESS","probability":0.91,"durationMs":960}}""";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private FakeUseCase useCase;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @TestConfiguration
    static class FakeUseCase {
        final List<HealthEvent> processed = new ArrayList<>();

        @Bean
        ProcessEventUseCase processEventUseCase() {
            return processed::add;
        }
    }

    @BeforeEach
    void forget() {
        useCase.processed.clear();
    }

    private org.springframework.test.web.servlet.ResultActions send(String body, String key) throws Exception {
        var request = post("/api/v1/ingestion/events").contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(key == null ? request : request.header("X-API-Key", key));
    }

    @Test
    void aBehaviorWindowIsAdaptedToTheDomainAndProcessedOnce() throws Exception {
        send(BEHAVIOR, "test-key")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("9b2c1e7a-0000-4000-8000-000000000001"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertThat(useCase.processed).singleElement().satisfies(event -> {
            assertThat(event.cageId()).isEqualTo("cage-1");
            assertThat(event.signal()).isInstanceOfSatisfying(BehaviorSignal.class, signal -> {
                assertThat(signal.color()).isEqualTo("RED");
                assertThat(signal.probAnomaly()).isEqualTo(0.81);
            });
        });
    }

    @Test
    void eachTypeGoesThroughItsOwnAdapter() throws Exception {
        send(AUDIO, "test-key").andExpect(status().isAccepted());

        assertThat(useCase.processed).singleElement()
                .extracting(HealthEvent::signal).isInstanceOf(AudioSignal.class);
    }

    @Test
    void aMissingOrWrongApiKeyIsA401AndNothingIsProcessed() throws Exception {
        send(BEHAVIOR, null).andExpect(status().isUnauthorized());
        send(BEHAVIOR, "wrong").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));

        assertThat(useCase.processed).isEmpty();
    }

    @Test
    void anEnvelopeThatBreaksTheContractIsA400AndNothingIsProcessed() throws Exception {
        send(BEHAVIOR.replace("\"stillSeconds\":48", "\"stillSeconds\":75"), "test-key")
                .andExpect(status().isBadRequest());
        send(BEHAVIOR.replace("\"type\":\"BEHAVIOR\"", "\"type\":\"SMELL\""), "test-key")
                .andExpect(status().isBadRequest());
        send(BEHAVIOR.replace("\"cageId\":\"cage-1\",", ""), "test-key")
                .andExpect(status().isBadRequest());

        assertThat(useCase.processed).isEmpty();
    }
}
