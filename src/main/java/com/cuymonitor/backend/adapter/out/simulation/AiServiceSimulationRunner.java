package com.cuymonitor.backend.adapter.out.simulation;

import com.cuymonitor.backend.domain.exception.SimulationBusyException;
import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import com.cuymonitor.backend.domain.port.out.SimulationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Talks to the AI service's demo endpoints, with the same API key as the events. */
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
            throw failure(ex);
        }
    }

    @Override
    public List<String> clipColors() {
        try {
            // read as a map: the clip also carries its credit and frame count, which the backend does not use
            Map<String, Object> clip = client.get()
                    .uri("/ai/v1/simulation/clip")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (clip == null || !(clip.get("colors") instanceof List<?> colors) || colors.isEmpty()) {
                throw SimulationUnavailableException.noClip();
            }
            return colors.stream().map(String::valueOf).toList();
        } catch (RestClientException ex) {
            throw failure(ex);
        }
    }

    @Override
    public void stop(String cageCode) {
        try {
            client.delete().uri("/ai/v1/simulation/{cageId}", cageCode).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            throw failure(ex);
        }
    }

    @Override
    public String status(String cageCode) {
        try {
            String body = client.get().uri("/ai/v1/simulation/{cageId}", cageCode).retrieve().body(String.class);
            return Objects.requireNonNullElse(body, "{}");
        } catch (RestClientException ex) {
            throw failure(ex);
        }
    }

    // 409 is the AI service's limit on running videos; 503 means it has no clip; anything else means it is down
    private static RuntimeException failure(RestClientException ex) {
        if (ex instanceof RestClientResponseException response) {
            if (response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) {
                return new SimulationBusyException();
            }
            if (response.getStatusCode().isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)) {
                return SimulationUnavailableException.noClip();
            }
        }
        return SimulationUnavailableException.aiServiceDown(ex);
    }
}
