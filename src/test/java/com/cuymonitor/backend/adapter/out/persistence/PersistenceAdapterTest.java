package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.adapter.out.persistence.entity.AlertJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.BaselineProfileJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.CageJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.EventJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.GuineaPigJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.StateTransitionJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.entity.WeightReadingJpaEntity;
import com.cuymonitor.backend.adapter.out.persistence.mapper.AlertPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.mapper.BaselineProfilePersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.mapper.CagePersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.mapper.EventPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.mapper.GuineaPigPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.mapper.StateTransitionPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.mapper.WeightReadingPersistenceMapper;
import com.cuymonitor.backend.adapter.out.persistence.repository.AlertJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.BaselineProfileJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.CageJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.EventJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.GuineaPigJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.StateTransitionJpaRepository;
import com.cuymonitor.backend.adapter.out.persistence.repository.WeightReadingJpaRepository;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AudioLabel;
import com.cuymonitor.backend.domain.model.AudioSignal;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.model.WeightReading;
import com.cuymonitor.backend.domain.model.WeightSignal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    @Test
    void cageAdapterMapsTheEntityReturnedByCodeLookup() {
        CageJpaRepository repository = mock(CageJpaRepository.class);
        when(repository.findByCode("cage-1")).thenReturn(Optional.of(cage()));
        CagePersistenceAdapter adapter = new CagePersistenceAdapter(repository, new CagePersistenceMapper());

        assertThat(adapter.findByCode("cage-1")).contains(new Cage(3L, "cage-1", "Ranchito"));
    }

    @Test
    void guineaPigAdapterResolvesCageAndPersistsTheMappedEntity() {
        CageJpaRepository cages = mock(CageJpaRepository.class);
        GuineaPigJpaRepository guineaPigs = mock(GuineaPigJpaRepository.class);
        when(cages.findByCode("cage-1")).thenReturn(Optional.of(cage()));
        when(guineaPigs.save(any(GuineaPigJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        GuineaPigPersistenceAdapter adapter =
                new GuineaPigPersistenceAdapter(cages, guineaPigs, new GuineaPigPersistenceMapper());

        GuineaPig saved = adapter.save(GuineaPig.register("cage-1", "Canela", MarkColor.RED, NOW));

        assertThat(saved.getName()).isEqualTo("Canela");
        assertThat(saved.getCageCode()).isEqualTo("cage-1");
        verify(guineaPigs).save(any(GuineaPigJpaEntity.class));
    }

    @Test
    void eventAdapterPersistsWeightEventsWithoutGuineaPigReferences() {
        CageJpaRepository cages = mock(CageJpaRepository.class);
        EventJpaRepository events = mock(EventJpaRepository.class);
        GuineaPigJpaRepository guineaPigs = mock(GuineaPigJpaRepository.class);
        when(cages.findByCode("cage-1")).thenReturn(Optional.of(cage()));
        when(events.save(any(EventJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EventPersistenceAdapter adapter = new EventPersistenceAdapter(
                cages, events, guineaPigs, new EventPersistenceMapper(JsonMapper.builder().build()));
        HealthEvent event = new HealthEvent(
                UUID.randomUUID(), "cage-1", NOW, "arduino", new WeightSignal(812.4, true));

        adapter.save(event, null);

        verify(events).save(org.mockito.ArgumentMatchers.argThat(
                saved -> saved.getType() == EventType.WEIGHT && saved.getGuineaPig() == null));
        verify(guineaPigs, never()).findById(8L);
    }

    @Test
    void eventAdapterRejectsGuineaPigFromAnotherCage() {
        CageJpaRepository cages = mock(CageJpaRepository.class);
        EventJpaRepository events = mock(EventJpaRepository.class);
        GuineaPigJpaRepository guineaPigs = mock(GuineaPigJpaRepository.class);
        when(cages.findByCode("cage-1")).thenReturn(Optional.of(cage()));
        when(guineaPigs.findById(8L)).thenReturn(Optional.of(guineaPig(cage(4L, "cage-2"))));
        EventPersistenceAdapter adapter = new EventPersistenceAdapter(
                cages, events, guineaPigs, new EventPersistenceMapper(JsonMapper.builder().build()));
        HealthEvent event = new HealthEvent(
                UUID.randomUUID(), "cage-1", NOW, "ai-service",
                new com.cuymonitor.backend.domain.model.BehaviorSignal(
                        MarkColor.RED, 60, 10, 1, 0, 0.2, 0.1, 0.9));

        assertThatThrownBy(() -> adapter.save(event, 8L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to cage");
        verify(events, never()).save(any(EventJpaEntity.class));
    }

    @Test
    void eventAdapterReturnsEmptyForZeroBehaviorLimitWithoutQuerying() {
        CageJpaRepository cages = mock(CageJpaRepository.class);
        EventJpaRepository events = mock(EventJpaRepository.class);
        GuineaPigJpaRepository guineaPigs = mock(GuineaPigJpaRepository.class);
        EventPersistenceAdapter adapter = new EventPersistenceAdapter(
                cages, events, guineaPigs, new EventPersistenceMapper(JsonMapper.builder().build()));

        assertThat(adapter.findLatestBehavior(8L, 0)).isEmpty();
        verifyNoInteractions(cages, events, guineaPigs);
    }

    @Test
    void eventAdapterRejectsNegativeBehaviorLimit() {
        CageJpaRepository cages = mock(CageJpaRepository.class);
        EventJpaRepository events = mock(EventJpaRepository.class);
        GuineaPigJpaRepository guineaPigs = mock(GuineaPigJpaRepository.class);
        EventPersistenceAdapter adapter = new EventPersistenceAdapter(
                cages, events, guineaPigs, new EventPersistenceMapper(JsonMapper.builder().build()));

        assertThatThrownBy(() -> adapter.findLatestBehavior(8L, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be non-negative");
        verifyNoInteractions(cages, events, guineaPigs);
    }

    @Test
    void alertAdapterPreservesDomainTimestampOnSave() {
        AlertJpaRepository alerts = mock(AlertJpaRepository.class);
        CageJpaRepository cages = mock(CageJpaRepository.class);
        GuineaPigJpaRepository guineaPigs = mock(GuineaPigJpaRepository.class);
        when(cages.findByCode("cage-1")).thenReturn(Optional.of(cage()));
        when(alerts.save(any(AlertJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AlertPersistenceAdapter adapter =
                new AlertPersistenceAdapter(alerts, cages, guineaPigs, new AlertPersistenceMapper());
        Alert alert = Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "sonido", NOW);

        assertThat(adapter.save(alert).getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void stateTransitionAdapterResolvesTheGuineaPigBeforeSaving() {
        GuineaPigJpaRepository guineaPigs = mock(GuineaPigJpaRepository.class);
        StateTransitionJpaRepository transitions = mock(StateTransitionJpaRepository.class);
        GuineaPigJpaEntity guineaPig = guineaPig(cage());
        when(guineaPigs.findById(8L)).thenReturn(Optional.of(guineaPig));
        when(transitions.save(any(StateTransitionJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        StateTransitionPersistenceAdapter adapter = new StateTransitionPersistenceAdapter(
                guineaPigs, transitions, new StateTransitionPersistenceMapper());
        StateTransition transition = new StateTransition(
                null, 8L, HealthStatus.NORMAL, HealthStatus.ALERT, "sin movimiento", NOW);

        assertThat(adapter.save(transition).guineaPigId()).isEqualTo(8L);
        verify(transitions).save(any(StateTransitionJpaEntity.class));
    }

    @Test
    void weightReadingAdapterResolvesCageAndReturnsSavedReading() {
        CageJpaRepository cages = mock(CageJpaRepository.class);
        WeightReadingJpaRepository readings = mock(WeightReadingJpaRepository.class);
        when(cages.findByCode("cage-1")).thenReturn(Optional.of(cage()));
        when(readings.save(any(WeightReadingJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        WeightReadingPersistenceAdapter adapter =
                new WeightReadingPersistenceAdapter(cages, readings, new WeightReadingPersistenceMapper());
        WeightReading reading = new WeightReading(null, "cage-1", 812.4, true, NOW);

        assertThat(adapter.save(reading).grams()).isEqualTo(812.4);
    }

    @Test
    void baselineProfileAdapterMapsFoundProfile() {
        BaselineProfileJpaRepository profiles = mock(BaselineProfileJpaRepository.class);
        BaselineProfileJpaEntity entity = new BaselineProfileJpaEntity(
                guineaPig(cage()), BigDecimal.valueOf(30), BigDecimal.ONE, BigDecimal.valueOf(0.3), NOW);
        when(profiles.findByGuineaPig_Id(8L)).thenReturn(Optional.of(entity));
        BaselineProfilePersistenceAdapter adapter =
                new BaselineProfilePersistenceAdapter(profiles, new BaselineProfilePersistenceMapper());

        assertThat(adapter.findByGuineaPigId(8L)).contains(new BaselineProfile(8L, 30, 1, 0.3, NOW));
    }

    @Test
    void eventAdapterSupportsCageLevelAudioEvent() {
        CageJpaRepository cages = mock(CageJpaRepository.class);
        EventJpaRepository events = mock(EventJpaRepository.class);
        GuineaPigJpaRepository guineaPigs = mock(GuineaPigJpaRepository.class);
        when(cages.findByCode("cage-1")).thenReturn(Optional.of(cage()));
        EventPersistenceMapper mapper = new EventPersistenceMapper(JsonMapper.builder().build());
        EventJpaEntity audioEntity = mapper.toEntity(
                new HealthEvent(UUID.randomUUID(), "cage-1", NOW, "ai-service",
                        new AudioSignal(AudioLabel.DISTRESS, 0.9, 1000)),
                cage(),
                null);
        when(events.findFirstByCage_CodeAndTypeOrderByOccurredAtDesc("cage-1", EventType.AUDIO))
                .thenReturn(Optional.of(audioEntity));
        EventPersistenceAdapter adapter = new EventPersistenceAdapter(cages, events, guineaPigs, mapper);

        assertThat(adapter.findLatest("cage-1", EventType.AUDIO))
                .contains(new HealthEvent(audioEntity.getId(), "cage-1", NOW, "ai-service",
                        new AudioSignal(AudioLabel.DISTRESS, 0.9, 1000)));
    }

    private static CageJpaEntity cage() {
        return cage(3L, "cage-1");
    }

    private static CageJpaEntity cage(long id, String code) {
        return new CageJpaEntity(id, code, "Ranchito", null);
    }

    private static GuineaPigJpaEntity guineaPig(CageJpaEntity cage) {
        return new GuineaPigJpaEntity(
                8L, cage, "Canela", MarkColor.RED, HealthStatus.NORMAL, NOW, true, NOW);
    }
}
