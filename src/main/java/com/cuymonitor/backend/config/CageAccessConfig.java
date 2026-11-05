package com.cuymonitor.backend.config;

import com.cuymonitor.backend.application.AlertHistoryService;
import com.cuymonitor.backend.application.AlertService;
import com.cuymonitor.backend.application.CageHealthService;
import com.cuymonitor.backend.application.CageOperationsService;
import com.cuymonitor.backend.application.CageService;
import com.cuymonitor.backend.application.CageWeightService;
import com.cuymonitor.backend.application.CageUseCases;
import com.cuymonitor.backend.application.GuineaPigHistoryService;
import com.cuymonitor.backend.application.GuineaPigService;
import com.cuymonitor.backend.domain.access.CageAccessProxy;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Proxy pattern wiring. The only {@link CageOperations} bean is the proxy; the real subject is created here with
 * {@code new} and handed to it, so no controller can reach the real subject and skip the owner check.
 */
@Configuration
public class CageAccessConfig {

    @Bean
    public CageOperations cageOperations(CageService cageService, CageHealthService cageHealthService,
                                         GuineaPigService guineaPigService, AlertService alertService,
                                         AlertHistoryService alertHistoryService,
                                         GuineaPigHistoryService historyService, CageWeightService weightService,
                                         CageRepository cages, AlertRepository alerts,
                                         GuineaPigRepository guineaPigs) {
        CageUseCases useCases = new CageUseCases(
                cageService,        // list cages
                cageService,        // get a cage
                cageService,        // create
                cageService,        // rename
                cageService,        // delete
                cageHealthService,  // health of a cage
                guineaPigService,   // list guinea pigs
                guineaPigService,   // register
                guineaPigService,   // delete
                guineaPigService,   // photo
                historyService,     // behavior history
                weightService,      // weight readings
                alertService,       // list alerts
                alertService,       // review an alert
                alertHistoryService); // alert history
        CageOperations realSubject = new CageOperationsService(useCases);
        return new CageAccessProxy(realSubject, cages, alerts, guineaPigs);
    }
}
