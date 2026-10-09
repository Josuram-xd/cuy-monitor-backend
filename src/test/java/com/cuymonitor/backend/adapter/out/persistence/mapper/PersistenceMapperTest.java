package com.cuymonitor.backend.adapter.out.persistence.mapper;

import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.CoatColor;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigBreed;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.model.WeightReading;
import com.cuymonitor.backend.domain.model.WeightSignal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceMapperTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");
    private static final CageJpaEntity CAGE_ENTITY = new CageJpaEntity(3L, "cage-1", "Ranchito", null);
    private static final GuineaPigJpaEntity GUINEA_PIG_ENTITY = new GuineaPigJpaEntity(
            8L, CAGE_ENTITY, "Canela", MarkColor.RED, HealthStatus.NORMAL, NOW, true, NOW);

    @Test
    void mapsCageBetweenDomainAndPersistence() {
        CagePersistenceMapper mapper = new CagePersistenceMapper();
        Cage cage = new Cage(3L, "cage-1", "Ranchito");

        assertThat(mapper.toDomain(mapper.toEntity(cage))).isEqualTo(cage);
    }

    @Test
    void mapsGuineaPigBetweenDomainAndPersistence() {
        GuineaPigPersistenceMapper mapper = new GuineaPigPersistenceMapper();
        GuineaPig guineaPig = GuineaPig.restore(
                8L, "cage-1", "Canela", MarkColor.RED, HealthStatus.NORMAL, NOW, true, NOW);

        assertThat(mapper.toDomain(mapper.toEntity(guineaPig, CAGE_ENTITY)).getName()).isEqualTo("Canela");
        assertThat(mapper.toDomain(mapper.toEntity(guineaPig, CAGE_ENTITY)).getId()).isEqualTo(8L);
    }

    @Test
    void mapsTheGuineaPigProfileBothWays() {
        GuineaPigPersistenceMapper mapper = new GuineaPigPersistenceMapper();
        GuineaPigProfile profile = new GuineaPigProfile(GuineaPigBreed.ABYSSINIAN, CoatColor.BICOLOR, 920, "activo");
        GuineaPig guineaPig = GuineaPig.restore(
                8L, "cage-1", "Canela", MarkColor.RED, HealthStatus.NORMAL, NOW, true, NOW, profile);

        GuineaPigJpaEntity entity = mapper.toEntity(guineaPig, CAGE_ENTITY);

        assertThat(entity.getBreed()).isEqualTo(GuineaPigBreed.ABYSSINIAN);
        assertThat(entity.getCoatColor()).isEqualTo(CoatColor.BICOLOR);
        assertThat(entity.getInitialWeightGrams()).isEqualTo(920);
        assertThat(entity.getNotes()).isEqualTo("activo");
        assertThat(mapper.toDomain(entity).getProfile()).isEqualTo(profile);
    }

    @Test
    void aGuineaPigStoredWithoutProfileComesBackWithAnEmptyOne() {
        GuineaPigPersistenceMapper mapper = new GuineaPigPersistenceMapper();

        assertThat(mapper.toDomain(GUINEA_PIG_ENTITY).getProfile()).isEqualTo(GuineaPigProfile.EMPTY);
    }

    @Test
    void mapsEventPayloadsForEachEventType() {
        EventPersistenceMapper mapper = new EventPersistenceMapper(JsonMapper.builder().build());
        List<HealthEvent> events = List.of(
                new HealthEvent(UUID.randomUUID(), "cage-1", NOW, "camera",
                        new BehaviorSignal(MarkColor.RED, 60, 10, 2, 1, 0.2, 0.1, 0.9)),
                new HealthEvent(UUID.randomUUID(), "cage-1", NOW, "microphone",
                        new AudioSignal(AudioLabel.DISTRESS, 0.95, 1000)),
                new HealthEvent(UUID.randomUUID(), "cage-1", NOW, "arduino",
                        new WeightSignal(812.4, true)));

        for (HealthEvent event : events) {
            GuineaPigJpaEntity guineaPig = event.type() == EventType.BEHAVIOR ? GUINEA_PIG_ENTITY : null;
            assertThat(mapper.toDomain(mapper.toEntity(event, CAGE_ENTITY, guineaPig))).isEqualTo(event);
        }
    }

    @Test
    void mapsAlertAndPreservesCreationAndReviewTimes() {
        AlertPersistenceMapper mapper = new AlertPersistenceMapper();
        Alert alert = Alert.restore(
                11L, "cage-1", null, HealthStatus.CRITICAL, EventType.AUDIO, "sonido de alarma",
                AlertStatus.REVIEWED, NOW, NOW.plusSeconds(60));

        Alert restored = mapper.toDomain(mapper.toEntity(alert, CAGE_ENTITY, null));

        assertThat(restored.getCreatedAt()).isEqualTo(NOW);
        assertThat(restored.getReviewedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(restored.getStatus()).isEqualTo(AlertStatus.REVIEWED);
        assertThat(restored.getId()).isEqualTo(11L);
    }

    @Test
    void mapsStateTransitionBetweenDomainAndPersistence() {
        StateTransitionPersistenceMapper mapper = new StateTransitionPersistenceMapper();
        StateTransition transition = new StateTransition(
                5L, 8L, HealthStatus.NORMAL, HealthStatus.ALERT, "sin movimiento", NOW);

        assertThat(mapper.toDomain(mapper.toEntity(transition, GUINEA_PIG_ENTITY))).isEqualTo(transition);
    }

    @Test
    void mapsWeightReadingBetweenDomainAndPersistence() {
        WeightReadingPersistenceMapper mapper = new WeightReadingPersistenceMapper();
        WeightReading reading = new WeightReading(4L, "cage-1", 812.4, true, NOW);

        assertThat(mapper.toDomain(mapper.toEntity(reading, CAGE_ENTITY))).isEqualTo(reading);
    }

    @Test
    void mapsBaselineProfileBetweenDomainAndPersistence() {
        BaselineProfilePersistenceMapper mapper = new BaselineProfilePersistenceMapper();
        BaselineProfile profile = new BaselineProfile(8L, 30.25, 1.5, 0.3001, NOW);

        assertThat(mapper.toDomain(mapper.toEntity(profile, GUINEA_PIG_ENTITY))).isEqualTo(profile);
    }
}
