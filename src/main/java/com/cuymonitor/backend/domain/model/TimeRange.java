package com.cuymonitor.backend.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A from-to window for history and weight queries (contract 3.4 and 3.7). It validates itself: the default is
 * the last 24 hours, {@code to} must be after {@code from}, and a range can't be longer than 31 days, so nobody
 * can ask for years of data in one request.
 */
public record TimeRange(Instant from, Instant to) {

    public static final Duration DEFAULT_LENGTH = Duration.ofHours(24);
    public static final Duration MAX_LENGTH = Duration.ofDays(31);

    public TimeRange {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("to must be after from");
        }
        if (Duration.between(from, to).compareTo(MAX_LENGTH) > 0) {
            throw new IllegalArgumentException("a range can't be longer than " + MAX_LENGTH.toDays() + " days");
        }
    }

    /** Fills what was not given: to defaults to now, from to 24 hours before to. */
    public static TimeRange of(Instant from, Instant to, Instant now) {
        return of(from, to, now, DEFAULT_LENGTH);
    }

    // the same, for a list that looks further back by default (the alert history: a week)
    public static TimeRange of(Instant from, Instant to, Instant now, Duration defaultLength) {
        Instant end = to == null ? now : to;
        Instant start = from == null ? end.minus(defaultLength) : from;
        return new TimeRange(start, end);
    }
}
