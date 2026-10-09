package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.InvalidEventException;
import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.HealthEvent;

// adapts the AUDIO payload: the ai-service classification of one clip of the cage
public class AudioClassificationAdapter implements EventSourceAdapter {

    @Override
    public HealthEvent adapt(IngestionEvent event) {
        PayloadReader payload = new PayloadReader(event.payload());
        try {
            AudioSignal clip = new AudioSignal(
                    payload.enumValue("label", AudioLabel.class),
                    payload.number("probability"),
                    payload.integer("durationMs"));
            return new HealthEvent(event.eventId(), event.cageId(), event.timestamp(), event.source(), clip);
        } catch (IllegalArgumentException ex) {
            throw new InvalidEventException("payload: " + ex.getMessage());
        }
    }
}
