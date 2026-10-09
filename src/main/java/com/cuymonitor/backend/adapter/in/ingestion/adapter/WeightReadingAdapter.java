package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.InvalidEventException;
import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.WeightSignal;

// adapts the WEIGHT payload sent by the Arduino serial bridge
public class WeightReadingAdapter implements EventSourceAdapter {

    @Override
    public HealthEvent adapt(IngestionEvent event) {
        PayloadReader payload = new PayloadReader(event.payload());
        try {
            WeightSignal reading = new WeightSignal(payload.number("grams"), payload.bool("stable"));
            return new HealthEvent(event.eventId(), event.cageId(), event.timestamp(), event.source(), reading);
        } catch (IllegalArgumentException ex) {
            throw new InvalidEventException("payload: " + ex.getMessage());
        }
    }
}
