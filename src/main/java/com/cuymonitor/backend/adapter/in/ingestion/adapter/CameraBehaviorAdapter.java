package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.InvalidEventException;
import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.MarkColor;

// adapts the BEHAVIOR payload computed by the ai-service from the camera
public class CameraBehaviorAdapter implements EventSourceAdapter {

    @Override
    public HealthEvent adapt(IngestionEvent event) {
        PayloadReader payload = new PayloadReader(event.payload());
        try {
            BehaviorSignal window = new BehaviorSignal(
                    payload.enumValue("color", MarkColor.class),
                    payload.integer("windowSeconds"),
                    payload.number("stillSeconds"),
                    payload.integer("feederVisits"),
                    payload.integer("watererVisits"),
                    payload.number("avgGroupDistance"),
                    payload.number("probAnomaly"),
                    payload.number("detectionConfidence"));
            return new HealthEvent(event.eventId(), event.cageId(), event.timestamp(), event.source(), window);
        } catch (IllegalArgumentException ex) {
            throw new InvalidEventException("payload: " + ex.getMessage());
        }
    }
}
