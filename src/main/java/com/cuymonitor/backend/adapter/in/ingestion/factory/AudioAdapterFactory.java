package com.cuymonitor.backend.adapter.in.ingestion.factory;

import com.cuymonitor.backend.adapter.in.ingestion.adapter.AudioClassificationAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.adapter.EventSourceAdapter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AudioAdapterFactory extends AdapterFactory {

    private final ObjectMapper objectMapper;

    public AudioAdapterFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public EventSourceAdapter createAdapter() {
        return new AudioClassificationAdapter(objectMapper);
    }
}
