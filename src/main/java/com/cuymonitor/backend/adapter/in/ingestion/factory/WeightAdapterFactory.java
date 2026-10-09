package com.cuymonitor.backend.adapter.in.ingestion.factory;

import com.cuymonitor.backend.adapter.in.ingestion.adapter.EventSourceAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.adapter.WeightReadingAdapter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class WeightAdapterFactory extends AdapterFactory {

    private final ObjectMapper objectMapper;

    public WeightAdapterFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public EventSourceAdapter createAdapter() {
        return new WeightReadingAdapter(objectMapper);
    }
}
