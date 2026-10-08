package com.cuymonitor.backend.adapter.in.ingestion.factory;

import com.cuymonitor.backend.adapter.in.ingestion.adapter.EventSourceAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.adapter.WeightReadingAdapter;

public class WeightAdapterFactory extends AdapterFactory {

    @Override
    protected EventSourceAdapter createAdapter() {
        return new WeightReadingAdapter();
    }
}
