package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.HealthEvent;

public interface EventSourceAdapter {
    HealthEvent adapt(IngestionEvent event);
}
