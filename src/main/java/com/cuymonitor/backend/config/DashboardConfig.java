package com.cuymonitor.backend.config;

import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import com.cuymonitor.backend.application.CageWeightService;
import com.cuymonitor.backend.application.GuineaPigHistoryService;
import com.cuymonitor.backend.application.AlertHistoryService;
import com.cuymonitor.backend.application.AlertService;
import com.cuymonitor.backend.application.CageHealthService;
import com.cuymonitor.backend.application.CageService;
import com.cuymonitor.backend.application.CatalogService;
import com.cuymonitor.backend.application.EventIngestionService;
import com.cuymonitor.backend.application.GuineaPigService;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.CageCodeGenerator;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.CatalogOptionRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires the use cases the dashboard reads and writes through. */
@Configuration
public class DashboardConfig {

    @Bean
    public CageService cageService(CageRepository cages, CageCodeGenerator codes, Clock clock) {
        return new CageService(cages, codes, clock);
    }

    @Bean
    public GuineaPigService guineaPigService(CageRepository cages, GuineaPigRepository guineaPigs,
                                             CatalogOptionRepository catalogOptions, Clock clock) {
        return new GuineaPigService(cages, guineaPigs, catalogOptions, clock);
    }

    @Bean
    public CatalogService catalogService(CatalogOptionRepository catalogOptions) {
        return new CatalogService(catalogOptions);
    }

    // stores each event once; EventProcessingService (DomainConfig) calls it before evaluating
    @Bean
    public EventIngestionService eventIngestionService(CageRepository cages, GuineaPigRepository guineaPigs,
                                                       EventRepository events, WeightReadingRepository weights) {
        return new EventIngestionService(cages, guineaPigs, events, weights);
    }

    @Bean
    public AlertService alertService(AlertRepository alerts, Clock clock) {
        return new AlertService(alerts, clock);
    }

    @Bean
    public AlertHistoryService alertHistoryService(AlertRepository alerts) {
        return new AlertHistoryService(alerts);
    }

    @Bean
    public CageHealthService cageHealthService(CageRepository cages, GuineaPigRepository guineaPigs,
                                               AlertRepository alerts, EventRepository events,
                                               WeightReadingRepository weights, Clock clock) {
        return new CageHealthService(cages, guineaPigs, alerts, events, weights, clock);
    }

    @Bean
    public GuineaPigHistoryService guineaPigHistoryService(GuineaPigRepository guineaPigs,
                                                           StateTransitionRepository transitions,
                                                           EventRepository events,
                                                           BaselineProfileRepository baselines) {
        return new GuineaPigHistoryService(guineaPigs, transitions, events, baselines);
    }

    @Bean
    public CageWeightService cageWeightService(CageRepository cages, WeightReadingRepository weights) {
        return new CageWeightService(cages, weights);
    }
}
