package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;

import java.util.Objects;
import java.util.Optional;

public final class EventHandlerContext {

    private final HealthEvent event;
    private GuineaPig guineaPig;
    private BaselineProfile baselineProfile;
    private String rejectionReason;
    private String anomalyReason;

    public EventHandlerContext(HealthEvent event) {
        this.event = Objects.requireNonNull(event, "event");
    }

    public HealthEvent event() {
        return event;
    }

    public boolean accepted() {
        return rejectionReason == null;
    }

    public Optional<String> rejectionReason() {
        return Optional.ofNullable(rejectionReason);
    }

    public void reject(String reason) {
        rejectionReason = Objects.requireNonNull(reason, "reason");
    }

    public Optional<GuineaPig> guineaPig() {
        return Optional.ofNullable(guineaPig);
    }

    public void identify(GuineaPig guineaPig) {
        this.guineaPig = Objects.requireNonNull(guineaPig, "guineaPig");
    }

    public Optional<BaselineProfile> baselineProfile() {
        return Optional.ofNullable(baselineProfile);
    }

    public void useBaselineProfile(BaselineProfile baselineProfile) {
        this.baselineProfile = Objects.requireNonNull(baselineProfile, "baselineProfile");
    }

    public Optional<String> anomalyReason() {
        return Optional.ofNullable(anomalyReason);
    }

    public void markAnomaly(String reason) {
        anomalyReason = Objects.requireNonNull(reason, "reason");
    }

    public void clearAnomaly() {
        anomalyReason = null;
    }
}
