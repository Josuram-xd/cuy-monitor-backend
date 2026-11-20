package com.cuymonitor.backend.adapter.out.simulation;

import com.cuymonitor.backend.domain.exception.SimulationBusyException;
import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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

    @Test
    void theClipColorsComeFromTheAiService() {
        server.expect(requestTo("http://ai-service:8000/ai/v1/simulation/clip"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", "the-key"))
                .andRespond(withSuccess("{\"colors\":[\"RED\",\"BLUE\"],\"credit\":{\"title\":\"t\"},\"frameCount\":3}",
                        MediaType.APPLICATION_JSON));

        assertThat(runner.clipColors()).containsExactly("RED", "BLUE");
        server.verify();
    }

    @Test
    void aClipThatIsNotThereIsUnavailable() {
        server.expect(requestTo("http://ai-service:8000/ai/v1/simulation/clip"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(runner::clipColors).isInstanceOf(SimulationUnavailableException.class)
                .hasMessageContaining("video");
    }

    @Test
    void aFullAiServiceIsBusy() {
        server.expect(requestTo("http://ai-service:8000/ai/v1/simulation")).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> runner.start("sim-0a1b2c3d", List.of("RED")))
                .isInstanceOf(SimulationBusyException.class);
    }

    @Test
    void stopDeletesTheRunOfTheCage() {
        server.expect(requestTo("http://ai-service:8000/ai/v1/simulation/sim-0a1b2c3d"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("X-API-Key", "the-key"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        runner.stop("sim-0a1b2c3d");
        server.verify();
    }

    @Test
    void statusIsTheAiServiceAnswerAsItIs() {
        server.expect(requestTo("http://ai-service:8000/ai/v1/simulation/sim-0a1b2c3d"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"running\":true,\"framesSent\":12}", MediaType.APPLICATION_JSON));

        assertThat(runner.status("sim-0a1b2c3d")).isEqualTo("{\"running\":true,\"framesSent\":12}");
        server.verify();
    }
}
