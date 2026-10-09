package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.in.ingestion.adapter.EventSourceAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.factory.AdapterFactorySelector;
import com.cuymonitor.backend.adapter.in.web.IngestionController;
import com.cuymonitor.backend.adapter.in.web.SystemController;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.WeightSignal;
import com.cuymonitor.backend.domain.port.in.ProcessEventUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SystemController.class, IngestionController.class},
        properties = {"app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!", "app.api-key=test-key"})
@Import({SecurityConfig.class, JwtConfig.class})
class SecurityConfigTest {

    private static final String EVENT = """
            {"eventId":"%s","type":"WEIGHT","cageId":"cage-1","timestamp":"2026-10-01T10:00:00Z",
             "source":"arduino","schemaVersion":1,"payload":{"grams":1200,"stable":true}}
            """.formatted(UUID.randomUUID());

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AdapterFactorySelector adapterFactorySelector;

    @MockitoBean
    private EventSourceAdapter eventSourceAdapter;

    @MockitoBean
    private ProcessEventUseCase processEventUseCase;

    @BeforeEach
    void configureIngestion() {
        given(adapterFactorySelector.createAdapterFor(EventType.WEIGHT)).willReturn(eventSourceAdapter);
        given(eventSourceAdapter.adapt(any())).willReturn(new HealthEvent(
                UUID.randomUUID(), "cage-1", Instant.parse("2026-10-01T10:00:00Z"), "arduino",
                new WeightSignal(1200, true)));
    }

    @Test
    void protectedRouteWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void protectedRouteWithValidTokenReturns200() throws Exception {
        given(jdbcTemplate.queryForObject(anyString(), any(Class.class))).willReturn(1);

        mvc.perform(get("/api/v1/system/status").header("Authorization", "Bearer " + token("cuy-monitor-backend")))
                .andExpect(status().isOk());
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() throws Exception {
        mvc.perform(get("/api/v1/system/status").header("Authorization", "Bearer " + token("someone-else")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenIsRejected() throws Exception {
        mvc.perform(get("/api/v1/system/status").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void ingestionStillWorksWithApiKeyAndNoToken() throws Exception {
        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isAccepted());
        verify(processEventUseCase).process(any(HealthEvent.class));
    }

    @Test
    void ingestionWithWrongApiKeyReturns401() throws Exception {
        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.message").value("invalid api key"));
    }

    @Test
    void ingestionRejectsInvalidPayloadWithoutCallingTheHealthCore() throws Exception {
        given(eventSourceAdapter.adapt(any())).willThrow(new IllegalArgumentException("invalid weight payload"));

        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.message").value("invalid weight payload"));

        verify(processEventUseCase, never()).process(any(HealthEvent.class));
    }

    private String token(String issuer) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
