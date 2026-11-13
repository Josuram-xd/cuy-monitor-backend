package com.cuymonitor.backend.adapter.out.simulation;

import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import com.cuymonitor.backend.domain.port.out.SimulationRunner;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** Asks the AI service ({@code POST /ai/v1/simulation}) to run the demo, with the same API key as the events. */
public class AiServiceSimulationRunner implements SimulationRunner {

    private final RestClient client;

    public AiServiceSimulationRunner(RestClient.Builder builder, String baseUrl, String apiKey) {
        this.client = builder.baseUrl(baseUrl).defaultHeader("X-API-Key", apiKey).build();
    }

    @Override
    public void start(String cageCode, List<String> markColors) {
        try {
            client.post()
                    .uri("/ai/v1/simulation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("cageId", cageCode, "colors", markColors))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw SimulationUnavailableException.aiServiceDown(ex);
        }
    }
}
