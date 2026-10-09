package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;

import java.time.Instant;
import java.util.List;

/** Shape of GET /api/v1/cages/{cageId}/health (docs/contracts/rest-api.md, 3.1). */
public record CageHealthResponse(String cageId, HealthStatus status, List<Member> guineaPigs, Audio audio,
                                 Weight weight, Instant updatedAt) {

    public record Member(long id, String name, MarkColor markColor, HealthStatus status) {

        static Member from(GuineaPig guineaPig) {
            return new Member(guineaPig.getId(), guineaPig.getName(), guineaPig.getMarkColor(),
                    guineaPig.getStatus());
        }
    }

    public record Audio(HealthStatus status, Instant lastEventAt) {
    }

    public record Weight(HealthStatus status, Double lastGrams, Instant lastMeasuredAt) {
    }

    public static CageHealthResponse from(CageHealthSummary summary) {
        return new CageHealthResponse(
                summary.cageCode(),
                summary.status(),
                summary.guineaPigs().stream().map(Member::from).toList(),
                new Audio(summary.audio().status(), summary.audio().lastEventAt()),
                new Weight(summary.weight().status(), summary.weight().lastGrams(),
                        summary.weight().lastMeasuredAt()),
                summary.updatedAt());
    }
}
