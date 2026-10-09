package com.cuymonitor.backend.adapter.in.ingestion.factory;

import com.cuymonitor.backend.adapter.in.ingestion.adapter.CameraBehaviorAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.adapter.EventSourceAdapter;

public class CameraAdapterFactory extends AdapterFactory {

    @Override
    protected EventSourceAdapter createAdapter() {
        return new CameraBehaviorAdapter();
    }
}
