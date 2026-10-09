package com.cuymonitor.backend.domain.health.composite;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;

import java.util.Objects;

public class GuineaPigHealth implements HealthComponent {

    private final GuineaPig guineaPig;

    public GuineaPigHealth(GuineaPig guineaPig) {
        this.guineaPig = Objects.requireNonNull(guineaPig, "guineaPig");
    }

    @Override
    public HealthStatus status() {
        return guineaPig.getStatus();
    }

    public GuineaPig guineaPig() {
        return guineaPig;
    }
}
