package com.cuymonitor.backend.config;

import com.cuymonitor.backend.application.EventProcessingService;
import com.cuymonitor.backend.domain.health.chain.BehaviorThresholdHandler;
import com.cuymonitor.backend.domain.health.chain.EventHandler;
import com.cuymonitor.backend.domain.health.chain.HandlerChainBuilder;
import com.cuymonitor.backend.domain.health.chain.IdentificationHandler;
import com.cuymonitor.backend.domain.health.chain.SustainedAnomalyHandler;
import com.cuymonitor.backend.domain.health.chain.ValidationHandler;
import com.cuymonitor.backend.domain.port.in.RecordEventUseCase;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the health core. Spring only creates the objects: the chain is linked by {@link HandlerChainBuilder}
 * (setNext, by hand), never by injected lists or @Order, so the Chain of Responsibility stays visible in code.
 */
@Configuration
@EnableConfigurationProperties(HealthProperties.class)
public class DomainConfig {

    @Bean
    public EventHandler healthEventChain(Clock clock, HealthProperties health, GuineaPigRepository guineaPigs,
                                         BaselineProfileRepository baselines, EventRepository events) {
        return new HandlerChainBuilder(
                new ValidationHandler(clock, health.maxEventAge(), health.minDetectionConfidence()),
                new IdentificationHandler(guineaPigs),
                new BehaviorThresholdHandler(baselines),
                new SustainedAnomalyHandler(events, health.consecutiveAnomalyWindows())
        ).build();
    }

    // the only ProcessEventUseCase: what IngestionController calls for every event
    @Bean
    public EventProcessingService eventProcessingService(RecordEventUseCase recorder, EventHandler healthEventChain,
                                                         GuineaPigRepository guineaPigs,
                                                         StateTransitionRepository transitions,
                                                         AlertRepository alerts, Clock clock,
                                                         HealthProperties health) {
        return new EventProcessingService(recorder, healthEventChain, guineaPigs, transitions, alerts, clock,
                health.distressProbability());
    }
}
