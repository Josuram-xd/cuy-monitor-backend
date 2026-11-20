package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** What the dashboard shows for a cage: its overall status and the parts it comes from. */
public record CageHealthSummary(String cageCode, HealthStatus status, List<GuineaPig> guineaPigs, Audio audio,
                                Weight weight, Instant updatedAt, Monitoring monitoring) {

    /** observation is the newest window of any guinea pig; guineaPigs has the newest one of each. */
    public record Monitoring(Instant connectedAt, Instant lastBehaviorAt, AudioLabel lastAudioLabel,
                             BehaviorSignal observation, List<LiveObservation> guineaPigs) {

        public Monitoring(Instant connectedAt, Instant lastBehaviorAt, AudioLabel lastAudioLabel,
                          BehaviorSignal observation) {
            this(connectedAt, lastBehaviorAt, lastAudioLabel, observation, List.of());
        }

        public Monitoring {
            guineaPigs = guineaPigs == null ? List.of() : List.copyOf(guineaPigs);
        }
    }

    /** The newest window of one guinea pig since the equipment was connected, and what stands out in it. */
    public record LiveObservation(long guineaPigId, String color, Instant at, BehaviorSignal window,
                                  List<BehaviorSymptom> symptoms) {

        public LiveObservation {
            symptoms = List.copyOf(symptoms);
        }
    }

    public CageHealthSummary(String cageCode, HealthStatus status, List<GuineaPig> guineaPigs, Audio audio,
                             Weight weight, Instant updatedAt) {
        this(cageCode, status, guineaPigs, audio, weight, updatedAt, null);
    }

    public CageHealthSummary {
        Objects.requireNonNull(cageCode, "cageCode");
        Objects.requireNonNull(status, "status");
        guineaPigs = List.copyOf(guineaPigs);
        Objects.requireNonNull(audio, "audio");
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** lastEventAt is null when no audio event arrived yet. */
    public record Audio(HealthStatus status, Instant lastEventAt) {
    }

    /** lastGrams and lastMeasuredAt are null when nothing was weighed yet. */
    public record Weight(HealthStatus status, Double lastGrams, Instant lastMeasuredAt) {
    }
}
