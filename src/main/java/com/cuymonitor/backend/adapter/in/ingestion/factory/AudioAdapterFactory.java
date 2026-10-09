package com.cuymonitor.backend.adapter.in.ingestion.factory;

import com.cuymonitor.backend.adapter.in.ingestion.adapter.AudioClassificationAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.adapter.EventSourceAdapter;

public class AudioAdapterFactory extends AdapterFactory {

    @Override
    protected EventSourceAdapter createAdapter() {
        return new AudioClassificationAdapter();
    }
}
