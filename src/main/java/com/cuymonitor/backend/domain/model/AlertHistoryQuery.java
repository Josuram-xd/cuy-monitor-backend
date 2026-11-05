package com.cuymonitor.backend.domain.model;

import java.util.Objects;
import java.util.Set;

// The alert history query as the repository runs it: the filter of the person, limited to cageCodes.
// No cage means nothing can be read, never "every cage".
public record AlertHistoryQuery(Set<String> cageCodes, Long guineaPigId, HealthStatus level, EventType type,
                                AlertStatus status, TimeRange range, Paging paging) {

    public AlertHistoryQuery {
        cageCodes = Set.copyOf(Objects.requireNonNull(cageCodes, "cageCodes"));
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(paging, "paging");
    }

    public boolean readsNothing() {
        return cageCodes.isEmpty();
    }

    // Information Expert: the query knows its criteria, so it tells whether one alert is part of the answer
    public boolean matches(Alert alert) {
        return cageCodes.contains(alert.getCageCode())
                && (guineaPigId == null || guineaPigId.equals(alert.getGuineaPigId()))
                && (level == null || level == alert.getLevel())
                && (type == null || type == alert.getType())
                && (status == null || status == alert.getStatus())
                && !alert.getCreatedAt().isBefore(range.from())
                && !alert.getCreatedAt().isAfter(range.to());
    }
}
