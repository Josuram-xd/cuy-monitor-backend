package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthSignal;
import com.cuymonitor.backend.domain.model.WeightSignal;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Template Method — ConcreteClass for the scale: only says what a WEIGHT payload looks like. */
public class WeightReadingAdapter extends PayloadAdapterTemplate {

    public WeightReadingAdapter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected EventType supportedType() {
        return EventType.WEIGHT;
    }

    @Override
    protected List<String> requiredFields() {
        return List.of("grams", "stable");
    }

    @Override
    protected Class<? extends HealthSignal> signalType() {
        return WeightSignal.class;
    }
}
