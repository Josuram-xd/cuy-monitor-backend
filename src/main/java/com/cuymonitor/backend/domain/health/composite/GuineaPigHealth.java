package com.cuymonitor.backend.domain.health.composite;

import com.cuymonitor.backend.domain.health.state.GuineaPigHealthContext;
import com.cuymonitor.backend.domain.model.HealthStatus;
import java.util.Objects;

public final class GuineaPigHealth implements HealthComponent {

    private final GuineaPigHealthContext context;

    public GuineaPigHealth(GuineaPigHealthContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public HealthStatus status() {
        return context.status();
    }
}
