package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.model.AlertHistoryFilter;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.Paging;
import com.cuymonitor.backend.domain.model.TimeRange;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// The query parameters of GET /api/v1/alerts/history as a domain filter. Whatever the domain refuses (a range
// longer than 31 days, a page of 500, level=NORMAL) is a 400, never a 500.
record AlertHistoryParams(String cageId, Long guineaPigId, HealthStatus level, EventType type, AlertStatus status,
                          Instant from, Instant to, Integer page, Integer size) {

    static final Duration DEFAULT_RANGE = Duration.ofDays(7);

    AlertHistoryFilter toFilter(Clock clock) {
        try {
            TimeRange range = TimeRange.of(from, to, clock.instant(), DEFAULT_RANGE);
            return new AlertHistoryFilter(blankToNull(cageId), guineaPigId, level, type, status, range,
                    Paging.of(page, size));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
