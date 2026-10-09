package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;

import java.time.Instant;

public record GuineaPigResponse(long id, String name, MarkColor markColor, HealthStatus status, Instant statusSince) {

    public static GuineaPigResponse from(GuineaPig guineaPig) {
        return new GuineaPigResponse(guineaPig.getId(), guineaPig.getName(), guineaPig.getMarkColor(),
                guineaPig.getStatus(), guineaPig.getStatusSince());
    }
}
