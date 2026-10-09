package com.cuymonitor.backend.domain.health.composite;

import com.cuymonitor.backend.domain.model.HealthStatus;
import java.util.List;
import java.util.Objects;

public final class CageHealth implements HealthComponent {

    private final List<HealthComponent> components;

    public CageHealth(List<HealthComponent> components) {
        this.components = List.copyOf(Objects.requireNonNull(components, "components"));
    }

    @Override
    public HealthStatus status() {
        return components.stream()
                .map(HealthComponent::status)
                .reduce(HealthStatus.NORMAL, HealthStatus::worst);
    }
}
