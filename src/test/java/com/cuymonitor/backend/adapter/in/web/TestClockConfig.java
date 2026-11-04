package com.cuymonitor.backend.adapter.in.web;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** A fixed clock for controller tests that take a time range ("the last 24 hours" depends on now). */
@TestConfiguration
public class TestClockConfig {

    public static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");

    @Bean
    Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
