package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.adapter.in.ingestion.factory.AdapterFactorySelector;
import com.cuymonitor.backend.adapter.in.ingestion.factory.AudioAdapterFactory;
import com.cuymonitor.backend.adapter.in.ingestion.factory.CameraAdapterFactory;
import com.cuymonitor.backend.adapter.in.ingestion.factory.WeightAdapterFactory;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightSignal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventSourceAdapterTest {

    private static final UUID EVENT_ID = UUID.fromString("3f1c2a5e-8f0b-4a53-9d0e-6b1c0a7e9a11");
    private static final Instant NOW = Instant.parse("2026-10-05T14:32:00Z");
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @Test
    void cameraAdapterConvertsBehaviorPayloadToHealthEvent() {
        IngestionEvent envelope = envelope(EventType.BEHAVIOR, Map.of(
                "color", "RED",
                "windowSeconds", 60,
                "stillSeconds", 48,
                "feederVisits", 0,
                "watererVisits", 1,
                "avgGroupDistance", 0.72,
                "probAnomaly", 0.81,
                "detectionConfidence", 0.93));

        HealthEvent actual = new CameraBehaviorAdapter(OBJECT_MAPPER).adapt(envelope);

        assertThat(actual).isEqualTo(new HealthEvent(
                EVENT_ID, "cage-1", NOW, "ai-service",
                new BehaviorSignal(MarkColor.RED, 60, 48, 0, 1, 0.72, 0.81, 0.93)));
    }

    @Test
    void audioAdapterConvertsAudioPayloadToHealthEvent() {
        IngestionEvent envelope = envelope(EventType.AUDIO, Map.of(
                "label", "DISTRESS", "probability", 0.88, "durationMs", 960));

        HealthEvent actual = new AudioClassificationAdapter(OBJECT_MAPPER).adapt(envelope);

        assertThat(actual).isEqualTo(new HealthEvent(
                EVENT_ID, "cage-1", NOW, "ai-service", new AudioSignal(AudioLabel.DISTRESS, 0.88, 960)));
    }

    @Test
    void weightAdapterConvertsWeightPayloadAndIgnoresUnknownFields() {
        IngestionEvent envelope = envelope(EventType.WEIGHT, Map.of(
                "grams", 812.4, "stable", true, "producerMetadata", "ignored"));

        HealthEvent actual = new WeightReadingAdapter(OBJECT_MAPPER).adapt(envelope);

        assertThat(actual).isEqualTo(new HealthEvent(
                EVENT_ID, "cage-1", NOW, "arduino", new WeightSignal(812.4, true)));
    }

    @Test
    void adapterRejectsMismatchedTypeAndMissingRequiredFields() {
        CameraBehaviorAdapter cameraAdapter = new CameraBehaviorAdapter(OBJECT_MAPPER);

        assertThatThrownBy(() -> cameraAdapter.adapt(envelope(EventType.AUDIO, Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not support event type");
        assertThatThrownBy(() -> new WeightReadingAdapter(OBJECT_MAPPER)
                .adapt(envelope(EventType.WEIGHT, Map.of("grams", 812.4))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stable");
    }

    @Test
    void adapterRejectsPayloadValuesOutsideDomainRules() {
        assertThatThrownBy(() -> new AudioClassificationAdapter(OBJECT_MAPPER)
                .adapt(envelope(EventType.AUDIO, Map.of(
                        "label", "NORMAL", "probability", 1.2, "durationMs", 960))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void factorySelectorUsesAConcreteFactoryForEachEventType() {
        AdapterFactorySelector selector = new AdapterFactorySelector(
                new CameraAdapterFactory(OBJECT_MAPPER),
                new AudioAdapterFactory(OBJECT_MAPPER),
                new WeightAdapterFactory(OBJECT_MAPPER));

        assertThat(selector.createAdapterFor(EventType.BEHAVIOR)).isInstanceOf(CameraBehaviorAdapter.class);
        assertThat(selector.createAdapterFor(EventType.AUDIO)).isInstanceOf(AudioClassificationAdapter.class);
        assertThat(selector.createAdapterFor(EventType.WEIGHT)).isInstanceOf(WeightReadingAdapter.class);
    }

    private static IngestionEvent envelope(EventType type, Map<String, Object> payload) {
        String source = type == EventType.WEIGHT ? "arduino" : "ai-service";
        return new IngestionEvent(EVENT_ID, type, "cage-1", NOW, source, 1, payload);
    }
}
