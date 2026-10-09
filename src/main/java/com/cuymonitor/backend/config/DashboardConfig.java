package com.cuymonitor.backend.config;

import com.cuymonitor.backend.application.CageHealthService;
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

    @Bean
    public CageHealthService cageHealthService(CageRepository cages, GuineaPigRepository guineaPigs,
                                               AlertRepository alerts, EventRepository events,
                                               WeightReadingRepository weights, Clock clock) {
        return new CageHealthService(cages, guineaPigs, alerts, events, weights, clock);
    }
}
