package com.cuymonitor.backend.config;

import com.cuymonitor.backend.application.AlertService;
import com.cuymonitor.backend.application.CageHealthService;
import com.cuymonitor.backend.application.CageOperationsService;
import com.cuymonitor.backend.application.CageService;
import com.cuymonitor.backend.application.CageUseCases;
import com.cuymonitor.backend.application.GuineaPigService;
import com.cuymonitor.backend.domain.access.CageAccessProxy;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.CageRepository;
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
                                         CageRepository cages, AlertRepository alerts) {
        CageUseCases useCases = new CageUseCases(cageService, cageService, cageService, cageService, cageService,
                cageHealthService, guineaPigService, guineaPigService, guineaPigService, guineaPigService, alertService,
                alertService);
        CageOperations realSubject = new CageOperationsService(useCases);
        return new CageAccessProxy(realSubject, cages, alerts);
    }
}
