package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;

import java.time.Instant;

public record GuineaPigResponse(Long id, String name, MarkColor markColor, HealthStatus status, Instant statusSince) {

    public static GuineaPigResponse from(GuineaPig pig) {
        return new GuineaPigResponse(pig.getId(), pig.getName(), pig.getMarkColor(), pig.getStatus(),
                pig.getStatusSince());
    }
}
