package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.in.web.IngestionController;
import com.cuymonitor.backend.adapter.in.web.SystemController;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SystemController.class, IngestionController.class},
        properties = {"app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!", "app.api-key=test-key"})
@Import({SecurityConfig.class, JwtConfig.class})
class SecurityConfigTest {

    private static final String EVENT = """
            {"eventId":"%s","type":"WEIGHT","cageId":"cage-1","timestamp":"2026-10-01T10:00:00Z",
             "source":"arduino","schemaVersion":1,"payload":{"grams":1200}}
            """.formatted(UUID.randomUUID());

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void protectedRouteWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/system/status")).andExpect(status().isUnauthorized());
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
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ingestionStillWorksWithApiKeyAndNoToken() throws Exception {
        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isAccepted());
    }

    @Test
    void ingestionWithWrongApiKeyReturns401() throws Exception {
        mvc.perform(post("/api/v1/ingestion/events").header("X-API-Key", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isUnauthorized());
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
