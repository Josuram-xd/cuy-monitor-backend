package com.cuymonitor.backend.domain.health.fake;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.StateTransition;
import com.cuymonitor.backend.domain.model.WeightReading;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// in-memory fakes of the health ports, shared by the domain and application tests
public final class HealthFakes {

    private HealthFakes() {
    }

    public static class Cages implements CageRepository {
        private final List<Cage> cages = new ArrayList<>(List.of(new Cage(1L, "cage-1", "Jaula piloto")));

        @Override
        public Optional<Cage> findByCode(String code) {
            return cages.stream().filter(c -> c.code().equals(code)).findFirst();
        }
    }

    public static class GuineaPigs implements GuineaPigRepository {
        private final Map<Long, GuineaPig> pigs = new HashMap<>();
        private long sequence;

        @Override
        public GuineaPig save(GuineaPig g) {
            long id = g.getId() != null ? g.getId() : ++sequence;
            GuineaPig stored = GuineaPig.restore(id, g.getCageCode(), g.getName(), g.getMarkColor(), g.getStatus(),
                    g.getStatusSince(), g.isActive(), g.getCreatedAt());
            pigs.put(id, stored);
            return findById(id).orElseThrow();
        }

        @Override
        public Optional<GuineaPig> findById(long id) {
            return Optional.ofNullable(pigs.get(id)).map(g -> GuineaPig.restore(g.getId(), g.getCageCode(),
                    g.getName(), g.getMarkColor(), g.getStatus(), g.getStatusSince(), g.isActive(), g.getCreatedAt()));
        }

        @Override
        public Optional<GuineaPig> findActiveByCageAndColor(String cageCode, MarkColor markColor) {
            return findActiveByCage(cageCode).stream().filter(g -> g.getMarkColor() == markColor).findFirst();
        }

        @Override
        public List<GuineaPig> findActiveByCage(String cageCode) {
            return pigs.keySet().stream().sorted().map(id -> findById(id).orElseThrow())
                    .filter(g -> g.isActive() && g.getCageCode().equals(cageCode)).toList();
        }
    }

    public static class Events implements EventRepository {
        private record Stored(HealthEvent event, Long guineaPigId) {
        }

        private final List<Stored> events = new ArrayList<>();

        @Override
        public boolean existsById(UUID eventId) {
            return events.stream().anyMatch(s -> s.event().eventId().equals(eventId));
        }

        @Override
        public void save(HealthEvent event, Long guineaPigId) {
            events.add(new Stored(event, guineaPigId));
        }

        @Override
        public List<HealthEvent> findLatestBehavior(long guineaPigId, int limit) {
            return events.stream()
                    .filter(s -> s.event().type() == EventType.BEHAVIOR && Objects.equals(s.guineaPigId(), guineaPigId))
                    .map(Stored::event)
                    .sorted(Comparator.comparing(HealthEvent::occurredAt).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public Optional<HealthEvent> findLatest(String cageCode, EventType type) {
            return events.stream().map(Stored::event)
                    .filter(e -> e.type() == type && e.cageId().equals(cageCode))
                    .max(Comparator.comparing(HealthEvent::occurredAt));
        }

        public int count() {
            return events.size();
        }
    }

    public static class Baselines implements BaselineProfileRepository {
        private final Map<Long, BaselineProfile> profiles = new HashMap<>();

        public void put(BaselineProfile profile) {
            profiles.put(profile.guineaPigId(), profile);
        }

        @Override
        public Optional<BaselineProfile> findByGuineaPigId(long guineaPigId) {
            return Optional.ofNullable(profiles.get(guineaPigId));
        }
    }

    public static class Transitions implements StateTransitionRepository {
        public final List<StateTransition> saved = new ArrayList<>();

        @Override
        public StateTransition save(StateTransition transition) {
            saved.add(transition);
            return transition;
        }
    }

    public static class Weights implements WeightReadingRepository {
        public final List<WeightReading> saved = new ArrayList<>();

        @Override
        public WeightReading save(WeightReading reading) {
            saved.add(reading);
            return reading;
        }

        @Override
        public Optional<WeightReading> findLatest(String cageCode) {
            return saved.stream().filter(r -> r.cageCode().equals(cageCode))
                    .max(Comparator.comparing(WeightReading::measuredAt));
        }
    }

    public static class Alerts implements AlertRepository {
        private final List<Alert> alerts = new ArrayList<>();

        @Override
        public Alert save(Alert alert) {
            if (alert.getId() == null) {
                alert.assignId(alerts.size() + 1);
                alerts.add(alert);
            }
            return alert;
        }

        @Override
        public List<Alert> findAll() {
            return alerts.reversed();
        }

        @Override
        public List<Alert> findByStatus(AlertStatus status) {
            return findAll().stream().filter(a -> a.getStatus() == status).toList();
        }
    }

    public static class RecordingObserver implements AlertObserver {
        public final List<Alert> received = new ArrayList<>();

        @Override
        public void onAlert(Alert alert) {
            received.add(alert);
        }
    }
}
