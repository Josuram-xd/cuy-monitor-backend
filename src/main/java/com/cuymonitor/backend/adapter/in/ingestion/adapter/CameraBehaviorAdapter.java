package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthSignal;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Template Method — ConcreteClass for the camera: only says what a BEHAVIOR payload looks like. */
public class CameraBehaviorAdapter extends PayloadAdapterTemplate {

    public CameraBehaviorAdapter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected EventType supportedType() {
        return EventType.BEHAVIOR;
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("color", "windowSeconds", "stillSeconds", "feederVisits", "watererVisits",
                "avgGroupDistance", "probAnomaly", "detectionConfidence");
    }

    @Override
    protected Class<? extends HealthSignal> signalType() {
        return BehaviorSignal.class;
    }
}
