package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.model.TimeRange;

import java.time.Clock;
import java.time.Instant;

/** Turns the optional from/to query parameters into a {@link TimeRange}; a bad range is a 400, not a 500. */
final class TimeRangeParams {

    private TimeRangeParams() {
    }

    static TimeRange of(Instant from, Instant to, Clock clock) {
        try {
            return TimeRange.of(from, to, clock.instant());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }
}
