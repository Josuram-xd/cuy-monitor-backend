package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthSignal;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Template Method — ConcreteClass for the microphone: only says what an AUDIO payload looks like. */
public class AudioClassificationAdapter extends PayloadAdapterTemplate {

    public AudioClassificationAdapter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected EventType supportedType() {
        return EventType.AUDIO;
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("label", "probability", "durationMs");
    }

    @Override
    protected Class<? extends HealthSignal> signalType() {
        return AudioSignal.class;
    }
}
