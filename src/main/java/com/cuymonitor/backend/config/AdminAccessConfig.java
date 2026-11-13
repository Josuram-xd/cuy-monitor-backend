package com.cuymonitor.backend.config;

import com.cuymonitor.backend.application.AdminService;
import com.cuymonitor.backend.domain.access.AdminAccessProxy;
import com.cuymonitor.backend.domain.access.AdminOperations;
import com.cuymonitor.backend.domain.port.out.CatalogOptionRepository;
import com.cuymonitor.backend.domain.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Proxy pattern wiring, the same shape as {@link CageAccessConfig}. The only {@link AdminOperations} bean is
 * the proxy; the real subject is built here with {@code new} and handed to it, so no controller can reach the
 * real service and skip the admin check.
 */
@Configuration
public class AdminAccessConfig {

    @Bean
    public AdminOperations adminOperations(CatalogOptionRepository catalog, UserRepository users, Clock clock) {
        AdminOperations realSubject = new AdminService(catalog, users, clock);
        return new AdminAccessProxy(realSubject, users);
    }
}
