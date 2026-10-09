package com.cuymonitor.backend.adapter.in.ingestion.factory;

import com.cuymonitor.backend.adapter.in.ingestion.InvalidEventException;
import com.cuymonitor.backend.adapter.in.ingestion.adapter.EventSourceAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;

// abstract creator: subclasses decide which adapter is created
public abstract class AdapterFactory {

    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    // the only switch on EventType: it picks the factory, never builds an adapter itself
    public static AdapterFactory forType(EventType type) {
        return switch (type) {
            case BEHAVIOR -> new CameraAdapterFactory();
            case AUDIO -> new AudioAdapterFactory();
            case WEIGHT -> new WeightAdapterFactory();
        };
    }

    protected abstract EventSourceAdapter createAdapter();

    public HealthEvent toHealthEvent(IngestionEvent event) {
        if (event.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw new InvalidEventException("unsupported schemaVersion " + event.schemaVersion());
        }
        return createAdapter().adapt(event);
    }
}
