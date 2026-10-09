package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;
import java.util.Objects;

public final class GuineaPigHealthContext {

    private final long guineaPigId;
    private HealthState state;

    public GuineaPigHealthContext(long guineaPigId, HealthState initialState) {
        this.guineaPigId = guineaPigId;
        this.state = Objects.requireNonNull(initialState, "initialState");
    }

    public long guineaPigId() {
        return guineaPigId;
    }

    public HealthState state() {
        return state;
    }

    public HealthStatus status() {
        return state.status();
    }

    void setState(HealthState state) {
        this.state = Objects.requireNonNull(state, "state");
    }
}
