package com.cuymonitor.backend.adapter.out.simulation;

import com.cuymonitor.backend.domain.port.out.LatestFrameSource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/** Reads the last JPEG the AI service kept for a cage ({@code GET /ai/v1/frames/latest}, 404 when there is none). */
public class AiServiceFrameSource implements LatestFrameSource {

    private final RestClient client;

    public AiServiceFrameSource(RestClient.Builder builder, String baseUrl, String apiKey) {
        this.client = builder.baseUrl(baseUrl).defaultHeader("X-API-Key", apiKey).build();
    }

    @Override
    public Optional<byte[]> latest(String aiCageId) {
        try {
            return Optional.ofNullable(client.get()
                    .uri("/ai/v1/frames/latest?cageId={cageId}", aiCageId)
                    .retrieve()
                    .body(byte[].class));
        } catch (RestClientException ex) {
            // the picture is optional: one that cannot be fetched is the same as one that has not arrived yet
            return Optional.empty();
        }
    }
}
