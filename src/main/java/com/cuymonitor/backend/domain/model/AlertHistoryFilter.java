package com.cuymonitor.backend.domain.model;

import java.util.Objects;
import java.util.Set;

// What the person asks the alert history for. Every criterion except the range and the page is optional (null).
// It does not say which cages can be read: that comes from the account, and within() turns it into the query.
public record AlertHistoryFilter(String cageCode, Long guineaPigId, HealthStatus level, EventType type,
                                 AlertStatus status, TimeRange range, Paging paging) {

    public AlertHistoryFilter {
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(paging, "paging");
        if (level != null && !level.raisesAlert()) {
            throw new IllegalArgumentException("an alert level is ALERT or CRITICAL");
        }
    }

    public boolean asksForCage() {
        return cageCode != null;
    }

    public boolean asksForGuineaPig() {
        return guineaPigId != null;
    }

    // Creator: the filter has every criterion, so it builds the query limited to the cages that can be read
    public AlertHistoryQuery within(Set<String> readableCages) {
        return new AlertHistoryQuery(readableCages, guineaPigId, level, type, status, range, paging);
    }
}
