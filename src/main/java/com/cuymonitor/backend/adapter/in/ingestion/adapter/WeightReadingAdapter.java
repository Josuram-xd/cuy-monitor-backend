package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.WeightSignal;
import tools.jackson.databind.ObjectMapper;

public class WeightReadingAdapter implements EventSourceAdapter {

    private final IngestionPayloadMapper payloadMapper;

    public WeightReadingAdapter(ObjectMapper objectMapper) {
        this.payloadMapper = new IngestionPayloadMapper(objectMapper);
    }

    @Override
    public HealthEvent adapt(IngestionEvent event) {
        WeightSignal signal = payloadMapper.toPayload(
                event, EventType.WEIGHT, WeightSignal.class, "grams", "stable");
        return new HealthEvent(event.eventId(), event.cageId(), event.timestamp(), event.source(), signal);
    }
}
