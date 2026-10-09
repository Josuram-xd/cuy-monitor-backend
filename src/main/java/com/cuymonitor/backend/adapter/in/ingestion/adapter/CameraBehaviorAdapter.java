package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import tools.jackson.databind.ObjectMapper;

public class CameraBehaviorAdapter implements EventSourceAdapter {

    private final IngestionPayloadMapper payloadMapper;

    public CameraBehaviorAdapter(ObjectMapper objectMapper) {
        this.payloadMapper = new IngestionPayloadMapper(objectMapper);
    }

    @Override
    public HealthEvent adapt(IngestionEvent event) {
        BehaviorSignal signal = payloadMapper.toPayload(
                event,
                EventType.BEHAVIOR,
                BehaviorSignal.class,
                "color",
                "windowSeconds",
                "stillSeconds",
                "feederVisits",
                "watererVisits",
                "avgGroupDistance",
                "probAnomaly",
                "detectionConfidence");
        return new HealthEvent(event.eventId(), event.cageId(), event.timestamp(), event.source(), signal);
    }
}
