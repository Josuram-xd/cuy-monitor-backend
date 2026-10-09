package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import tools.jackson.databind.ObjectMapper;

public class AudioClassificationAdapter implements EventSourceAdapter {

    private final IngestionPayloadMapper payloadMapper;

    public AudioClassificationAdapter(ObjectMapper objectMapper) {
        this.payloadMapper = new IngestionPayloadMapper(objectMapper);
    }

    @Override
    public HealthEvent adapt(IngestionEvent event) {
        AudioSignal signal = payloadMapper.toPayload(
                event, EventType.AUDIO, AudioSignal.class, "label", "probability", "durationMs");
        return new HealthEvent(event.eventId(), event.cageId(), event.timestamp(), event.source(), signal);
    }
}
