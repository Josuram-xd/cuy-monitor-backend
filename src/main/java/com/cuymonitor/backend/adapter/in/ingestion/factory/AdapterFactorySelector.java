package com.cuymonitor.backend.adapter.in.ingestion.factory;

import com.cuymonitor.backend.adapter.in.ingestion.adapter.EventSourceAdapter;
import com.cuymonitor.backend.domain.model.EventType;
import org.springframework.stereotype.Component;

@Component
public class AdapterFactorySelector {

    private final CameraAdapterFactory cameraAdapterFactory;
    private final AudioAdapterFactory audioAdapterFactory;
    private final WeightAdapterFactory weightAdapterFactory;

    public AdapterFactorySelector(CameraAdapterFactory cameraAdapterFactory, AudioAdapterFactory audioAdapterFactory,
                                  WeightAdapterFactory weightAdapterFactory) {
        this.cameraAdapterFactory = cameraAdapterFactory;
        this.audioAdapterFactory = audioAdapterFactory;
        this.weightAdapterFactory = weightAdapterFactory;
    }

    public EventSourceAdapter createAdapterFor(EventType type) {
        AdapterFactory factory = switch (type) {
            case BEHAVIOR -> cameraAdapterFactory;
            case AUDIO -> audioAdapterFactory;
            case WEIGHT -> weightAdapterFactory;
        };
        return factory.createAdapter();
    }
}
