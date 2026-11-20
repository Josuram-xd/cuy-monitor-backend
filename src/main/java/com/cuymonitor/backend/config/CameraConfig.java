package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.out.simulation.AiServiceFrameSource;
import com.cuymonitor.backend.application.CameraSnapshotService;
import com.cuymonitor.backend.domain.port.in.GetLatestCameraFrameUseCase;
import com.cuymonitor.backend.domain.port.out.HardwareStation;
import com.cuymonitor.backend.domain.port.out.LatestFrameSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Wires the live camera picture: the AI service keeps the last frame of each cage, the backend serves it. */
@Configuration
public class CameraConfig {

    @Bean
    public LatestFrameSource latestFrameSource(@Value("${app.ai-service.url}") String url,
                                               @Value("${app.api-key}") String apiKey) {
        // short timeouts: a picture is refreshed every few seconds, a slow one is not worth waiting for
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(3));
        requests.setReadTimeout(Duration.ofSeconds(5));
        return new AiServiceFrameSource(RestClient.builder().requestFactory(requests), url, apiKey);
    }

    @Bean
    public GetLatestCameraFrameUseCase cameraSnapshotService(HardwareStation station, LatestFrameSource frames) {
        return new CameraSnapshotService(station, frames);
    }
}
