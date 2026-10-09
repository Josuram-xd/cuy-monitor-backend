package com.cuymonitor.backend.adapter.in.ingestion.factory;

import com.cuymonitor.backend.adapter.in.ingestion.adapter.CameraBehaviorAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.adapter.EventSourceAdapter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CameraAdapterFactory extends AdapterFactory {

    private final ObjectMapper objectMapper;

    public CameraAdapterFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public EventSourceAdapter createAdapter() {
        return new CameraBehaviorAdapter(objectMapper);
    }
}
