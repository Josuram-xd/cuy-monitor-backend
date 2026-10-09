package com.cuymonitor.backend.domain.health.composite;

import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

// cage alarm while the last clip was a clear distress call heard in the last few minutes
public class CageAudioHealth implements HealthComponent {

    static final double DISTRESS_THRESHOLD = 0.8;
    static final Duration RECENT = Duration.ofMinutes(5);

    private final HealthEvent lastAudio;
    private final Instant now;

    public CageAudioHealth(Optional<HealthEvent> lastAudio, Instant now) {
        this.lastAudio = lastAudio.orElse(null);
        this.now = now;
    }

    @Override
    public HealthStatus status() {
        if (lastAudio == null || !(lastAudio.signal() instanceof AudioSignal clip)) {
            return HealthStatus.NORMAL;
        }
        boolean distress = clip.label() == AudioLabel.DISTRESS && clip.probability() >= DISTRESS_THRESHOLD;
        boolean recent = !lastAudio.occurredAt().isBefore(now.minus(RECENT));
        return distress && recent ? HealthStatus.ALERT : HealthStatus.NORMAL;
    }

    public Optional<Instant> lastEventAt() {
        return Optional.ofNullable(lastAudio).map(HealthEvent::occurredAt);
    }
}
