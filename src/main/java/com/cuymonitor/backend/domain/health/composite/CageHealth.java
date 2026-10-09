package com.cuymonitor.backend.domain.health.composite;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.WeightReading;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CageHealth implements HealthComponent {

    private final String cageCode;
    private final List<HealthComponent> children = new ArrayList<>();

    public CageHealth(String cageCode) {
        this.cageCode = Objects.requireNonNull(cageCode, "cageCode");
    }

    public static CageHealth of(String cageCode, List<GuineaPig> guineaPigs, Optional<HealthEvent> lastAudio,
                                Optional<WeightReading> lastWeight, Instant now) {
        CageHealth cage = new CageHealth(cageCode);
        guineaPigs.forEach(pig -> cage.add(new GuineaPigHealth(pig)));
        cage.add(new CageAudioHealth(lastAudio, now));
        cage.add(new CageWeightHealth(lastWeight));
        return cage;
    }

    public void add(HealthComponent child) {
        children.add(Objects.requireNonNull(child, "child"));
    }

    // the cage is as bad as its worst part
    @Override
    public HealthStatus status() {
        HealthStatus worst = HealthStatus.NORMAL;
        for (HealthComponent child : children) {
            worst = HealthStatus.worst(worst, child.status());
        }
        return worst;
    }

    public String cageCode() {
        return cageCode;
    }

    public List<GuineaPigHealth> guineaPigs() {
        return children.stream()
                .filter(GuineaPigHealth.class::isInstance)
                .map(GuineaPigHealth.class::cast)
                .toList();
    }

    public Optional<CageAudioHealth> audio() {
        return children.stream().filter(CageAudioHealth.class::isInstance).map(CageAudioHealth.class::cast)
                .findFirst();
    }

    public Optional<CageWeightHealth> weight() {
        return children.stream().filter(CageWeightHealth.class::isInstance).map(CageWeightHealth.class::cast)
                .findFirst();
    }
}
