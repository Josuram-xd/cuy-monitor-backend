package com.cuymonitor.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/** The ai-service asks for the guinea pig references of a cage every few minutes: an unchanged list is a 304. */
@Configuration
public class ReferenceEtagConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> referenceEtagFilter() {
        var registration = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/v1/ingestion/cages/*");
        return registration;
    }
}
