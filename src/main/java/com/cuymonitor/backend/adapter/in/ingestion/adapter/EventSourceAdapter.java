package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.HealthEvent;

// target of the Adapter pattern: what the core understands, whatever the producer sent
public interface EventSourceAdapter {

    HealthEvent adapt(IngestionEvent event);
}
