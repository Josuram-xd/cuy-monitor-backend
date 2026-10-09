package com.cuymonitor.backend.adapter.in.ingestion.factory;

import com.cuymonitor.backend.adapter.in.ingestion.adapter.EventSourceAdapter;

public abstract class AdapterFactory {
    public abstract EventSourceAdapter createAdapter();
}
