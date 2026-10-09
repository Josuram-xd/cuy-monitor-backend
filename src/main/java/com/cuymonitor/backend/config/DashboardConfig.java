package com.cuymonitor.backend.config;

import com.cuymonitor.backend.application.AlertService;
import com.cuymonitor.backend.application.CageHealthService;
import com.cuymonitor.backend.application.EventIngestionService;
import com.cuymonitor.backend.application.GuineaPigService;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.CageRepository;
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
    public GuineaPigService guineaPigService(CageRepository cages, GuineaPigRepository guineaPigs, Clock clock) {
        return new GuineaPigService(cages, guineaPigs, clock);
    }

    // Stores events only. Replace it with the health core's EventProcessingService when that is merged.
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
    public CageHealthService cageHealthService(CageRepository cages, GuineaPigRepository guineaPigs,
                                               AlertRepository alerts, EventRepository events,
                                               WeightReadingRepository weights, Clock clock) {
        return new CageHealthService(cages, guineaPigs, alerts, events, weights, clock);
    }
}
