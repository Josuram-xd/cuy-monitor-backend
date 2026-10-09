package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.InvalidEventException;
import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightSignal;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventSourceAdaptersTest {

    private static final UUID EVENT_ID = UUID.fromString("3f1c2a5e-8f0b-4a53-9d0e-6b1c0a7e9a11");
    private static final Instant AT = Instant.parse("2026-10-05T14:32:00Z");

    @Test
    void cameraAdapterTurnsTheBehaviorPayloadIntoAWindow() {
        HealthEvent event = new CameraBehaviorAdapter().adapt(envelope(EventType.BEHAVIOR, behaviorPayload()));

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.cageId()).isEqualTo("cage-1");
        assertThat(event.occurredAt()).isEqualTo(AT);
        assertThat(event.source()).isEqualTo("ai-service");
        assertThat(event.signal()).isEqualTo(
                new BehaviorSignal(MarkColor.RED, 60, 48, 0, 1, 0.72, 0.81, 0.93));
    }

    @Test
    void cameraAdapterAcceptsLowercaseColors() {
        Map<String, Object> payload = behaviorPayload();
        payload.put("color", "red");

        HealthEvent event = new CameraBehaviorAdapter().adapt(envelope(EventType.BEHAVIOR, payload));

        assertThat(((BehaviorSignal) event.signal()).color()).isEqualTo(MarkColor.RED);
    }

    @Test
    void cameraAdapterExplainsAMissingField() {
        Map<String, Object> payload = behaviorPayload();
        payload.remove("stillSeconds");

        assertThatThrownBy(() -> new CameraBehaviorAdapter().adapt(envelope(EventType.BEHAVIOR, payload)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessage("payload.stillSeconds is required");
    }

    @Test
    void cameraAdapterRejectsAnUnknownColor() {
        Map<String, Object> payload = behaviorPayload();
        payload.put("color", "PINK");

        assertThatThrownBy(() -> new CameraBehaviorAdapter().adapt(envelope(EventType.BEHAVIOR, payload)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("payload.color");
    }

    @Test
    void cameraAdapterRejectsValuesOutsideTheContract() {
        Map<String, Object> payload = behaviorPayload();
        payload.put("stillSeconds", 75);

        assertThatThrownBy(() -> new CameraBehaviorAdapter().adapt(envelope(EventType.BEHAVIOR, payload)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("stillSeconds");
    }

    @Test
    void cameraAdapterRejectsDecimalVisits() {
        Map<String, Object> payload = behaviorPayload();
        payload.put("feederVisits", 1.5);

        assertThatThrownBy(() -> new CameraBehaviorAdapter().adapt(envelope(EventType.BEHAVIOR, payload)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessage("payload.feederVisits must be a whole number");
    }

    @Test
    void audioAdapterTurnsTheClassificationIntoASignal() {
        HealthEvent event = new AudioClassificationAdapter().adapt(envelope(EventType.AUDIO,
                new HashMap<>(Map.of("label", "DISTRESS", "probability", 0.88, "durationMs", 960))));

        assertThat(event.signal()).isEqualTo(new AudioSignal(AudioLabel.DISTRESS, 0.88, 960));
    }

    @Test
    void audioAdapterRejectsATextProbability() {
        assertThatThrownBy(() -> new AudioClassificationAdapter().adapt(envelope(EventType.AUDIO,
                new HashMap<>(Map.of("label", "DISTRESS", "probability", "high", "durationMs", 960)))))
                .isInstanceOf(InvalidEventException.class)
                .hasMessage("payload.probability must be a number");
    }

    @Test
    void weightAdapterTurnsTheReadingIntoASignal() {
        HealthEvent event = new WeightReadingAdapter().adapt(envelope(EventType.WEIGHT,
                new HashMap<>(Map.of("grams", 812.4, "stable", true))));

        assertThat(event.signal()).isEqualTo(new WeightSignal(812.4, true));
        assertThat(event.type()).isEqualTo(EventType.WEIGHT);
    }

    @Test
    void weightAdapterNeedsAStableFlag() {
        assertThatThrownBy(() -> new WeightReadingAdapter().adapt(envelope(EventType.WEIGHT,
                new HashMap<>(Map.of("grams", 812.4, "stable", "yes")))))
                .isInstanceOf(InvalidEventException.class)
                .hasMessage("payload.stable must be true or false");
    }

    private static Map<String, Object> behaviorPayload() {
        return new HashMap<>(Map.of(
                "color", "RED", "windowSeconds", 60, "stillSeconds", 48, "feederVisits", 0, "watererVisits", 1,
                "avgGroupDistance", 0.72, "probAnomaly", 0.81, "detectionConfidence", 0.93));
    }

    private static IngestionEvent envelope(EventType type, Map<String, Object> payload) {
        return new IngestionEvent(EVENT_ID, type, "cage-1", AT, "ai-service", 1, payload);
    }
}
