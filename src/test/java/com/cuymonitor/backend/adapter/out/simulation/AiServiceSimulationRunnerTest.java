package com.cuymonitor.backend.adapter.out.simulation;

import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class AiServiceSimulationRunnerTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AiServiceSimulationRunner runner =
            new AiServiceSimulationRunner(builder, "http://ai-service:8000", "the-key");

    @Test
    void asksTheAiServiceWithTheApiKeyAndTheMarks() {
        server.expect(requestTo("http://ai-service:8000/ai/v1/simulation"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "the-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"cageId\":\"cage-1\",\"colors\":[\"RED\",\"BLUE\"]}"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED));

        runner.start("cage-1", List.of("RED", "BLUE"));

        server.verify();
    }

    @Test
    void anAiServiceErrorMeansTheDemoIsUnavailable() {
        server.expect(requestTo("http://ai-service:8000/ai/v1/simulation")).andRespond(withServerError());

        assertThatThrownBy(() -> runner.start("cage-1", List.of("RED")))
                .isInstanceOf(SimulationUnavailableException.class);
    }
}
