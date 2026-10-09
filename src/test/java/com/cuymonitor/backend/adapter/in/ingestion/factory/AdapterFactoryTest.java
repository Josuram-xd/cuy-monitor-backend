package com.cuymonitor.backend.adapter.in.ingestion.factory;

import com.cuymonitor.backend.adapter.in.ingestion.InvalidEventException;
import com.cuymonitor.backend.adapter.in.ingestion.adapter.AudioClassificationAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.adapter.CameraBehaviorAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.adapter.WeightReadingAdapter;
import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.WeightSignal;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdapterFactoryTest {

    @Test
    void behaviorUsesTheCameraFactory() {
        AdapterFactory factory = AdapterFactory.forType(EventType.BEHAVIOR);

        assertThat(factory).isInstanceOf(CameraAdapterFactory.class);
        assertThat(factory.createAdapter()).isInstanceOf(CameraBehaviorAdapter.class);
    }

    @Test
    void audioUsesTheAudioFactory() {
        AdapterFactory factory = AdapterFactory.forType(EventType.AUDIO);

        assertThat(factory).isInstanceOf(AudioAdapterFactory.class);
        assertThat(factory.createAdapter()).isInstanceOf(AudioClassificationAdapter.class);
    }

    @Test
    void weightUsesTheWeightFactory() {
        AdapterFactory factory = AdapterFactory.forType(EventType.WEIGHT);

        assertThat(factory).isInstanceOf(WeightAdapterFactory.class);
        assertThat(factory.createAdapter()).isInstanceOf(WeightReadingAdapter.class);
    }

    @Test
    void toHealthEventRunsTheCreatedAdapter() {
        HealthEvent event = AdapterFactory.forType(EventType.WEIGHT).toHealthEvent(weightEvent(1));

        assertThat(event.signal()).isEqualTo(new WeightSignal(812.4, true));
    }

    @Test
    void rejectsAnotherSchemaVersion() {
        assertThatThrownBy(() -> AdapterFactory.forType(EventType.WEIGHT).toHealthEvent(weightEvent(2)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("schemaVersion 2");
    }

    private static IngestionEvent weightEvent(int schemaVersion) {
        return new IngestionEvent(UUID.randomUUID(), EventType.WEIGHT, "cage-1",
                Instant.parse("2026-10-05T14:00:00Z"), "arduino", schemaVersion,
                Map.of("grams", 812.4, "stable", true));
    }
}
