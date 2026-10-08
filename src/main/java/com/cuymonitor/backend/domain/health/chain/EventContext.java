package com.cuymonitor.backend.domain.health.chain;

import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;

import java.util.Objects;
import java.util.Optional;

// travels through the chain; each handler reads what the previous ones left and adds its own result
public class EventContext {

    private final HealthEvent event;
    private String dropReason;
    private GuineaPig guineaPig;
    private BaselineProfile baseline;
    private String anomalyReason;
    private boolean sustainedAnomaly;
    private boolean sustainedRecovery;

    public EventContext(HealthEvent event) {
        this.event = Objects.requireNonNull(event, "event");
    }

    public HealthEvent event() {
        return event;
    }

    public void drop(String reason) {
        dropReason = Objects.requireNonNull(reason, "reason");
    }

    public boolean isDropped() {
        return dropReason != null;
    }

    public String dropReason() {
        return dropReason;
    }

    public void identify(GuineaPig pig) {
        guineaPig = Objects.requireNonNull(pig, "pig");
    }

    public Optional<GuineaPig> guineaPig() {
        return Optional.ofNullable(guineaPig);
    }

    public void useBaseline(BaselineProfile profile) {
        baseline = Objects.requireNonNull(profile, "profile");
    }

    public Optional<BaselineProfile> baseline() {
        return Optional.ofNullable(baseline);
    }

    public void markAnomalous(String reason) {
        anomalyReason = Objects.requireNonNull(reason, "reason");
    }

    public boolean isAnomalous() {
        return anomalyReason != null;
    }

    public String anomalyReason() {
        return anomalyReason;
    }

    public void confirmSustainedAnomaly() {
        sustainedAnomaly = true;
    }

    public boolean isSustainedAnomaly() {
        return sustainedAnomaly;
    }

    public void confirmSustainedRecovery() {
        sustainedRecovery = true;
    }

    public boolean isSustainedRecovery() {
        return sustainedRecovery;
    }
}
