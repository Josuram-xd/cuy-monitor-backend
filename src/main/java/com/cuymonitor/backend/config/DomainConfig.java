package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.out.notification.DatabaseAlertObserver;
import com.cuymonitor.backend.adapter.out.notification.LogAlertObserver;
import com.cuymonitor.backend.adapter.out.notification.WebSocketAlertObserver;
import com.cuymonitor.backend.application.EventProcessingService;
import com.cuymonitor.backend.domain.health.chain.ChainSettings;
import com.cuymonitor.backend.domain.health.chain.EventHandler;
import com.cuymonitor.backend.domain.health.chain.HandlerChainBuilder;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class DomainConfig {

    // first values for the pilot; Task 11.4 moves them to application.yml after the farm test
    static final ChainSettings CHAIN_SETTINGS = new ChainSettings(Duration.ofHours(1), 0.5, 3);

    @Bean
    public AlertPublisher alertPublisher(DatabaseAlertObserver databaseObserver,
                                         WebSocketAlertObserver webSocketObserver,
                                         LogAlertObserver logObserver) {
        AlertPublisher publisher = new AlertPublisher();
        // database first: it gives the alert its id before the others send it
        publisher.subscribe(databaseObserver);
        publisher.subscribe(webSocketObserver);
        publisher.subscribe(logObserver);
        return publisher;
    }

    @Bean
    public EventHandler eventHandlerChain(Clock clock, GuineaPigRepository guineaPigRepository,
                                          BaselineProfileRepository baselineProfileRepository,
                                          EventRepository eventRepository) {
        return new HandlerChainBuilder(clock, guineaPigRepository, baselineProfileRepository, eventRepository,
                CHAIN_SETTINGS).build();
    }

    @Bean
    public EventProcessingService eventProcessingService(CageRepository cageRepository,
                                                         GuineaPigRepository guineaPigRepository,
                                                         EventRepository eventRepository,
                                                         StateTransitionRepository stateTransitionRepository,
                                                         WeightReadingRepository weightReadingRepository,
                                                         EventHandler eventHandlerChain,
                                                         AlertPublisher alertPublisher, Clock clock) {
        return new EventProcessingService(cageRepository, guineaPigRepository, eventRepository,
                stateTransitionRepository, weightReadingRepository, eventHandlerChain, alertPublisher, clock);
    }
}
