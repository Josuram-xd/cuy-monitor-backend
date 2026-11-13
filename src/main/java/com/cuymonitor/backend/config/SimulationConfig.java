package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.out.simulation.AiServiceSimulationRunner;
import com.cuymonitor.backend.application.SimulationService;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.port.out.SimulationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Wires the "Simulación" button: the backend asks the AI service, which it reaches inside the Compose network. */
@Configuration
public class SimulationConfig {

    @Bean
    public SimulationRunner simulationRunner(@Value("${app.ai-service.url}") String url,
                                             @Value("${app.api-key}") String apiKey) {
        // short timeouts: the AI service only starts the demo and answers 202 at once
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(3));
        requests.setReadTimeout(Duration.ofSeconds(10));
        return new AiServiceSimulationRunner(RestClient.builder().requestFactory(requests), url, apiKey);
    }

    @Bean
    public SimulationService simulationService(CageOperations cageOperations, SimulationRunner simulationRunner) {
        return new SimulationService(cageOperations, simulationRunner);
    }
}
