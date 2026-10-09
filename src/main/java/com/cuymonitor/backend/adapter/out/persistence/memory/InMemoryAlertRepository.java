package com.cuymonitor.backend.adapter.out.persistence.memory;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("memory")
public class InMemoryAlertRepository implements AlertRepository {

    private static final Comparator<Alert> NEWEST_FIRST =
            Comparator.comparing(Alert::getCreatedAt).thenComparing(Alert::getId).reversed();

    private final Map<Long, Alert> alerts = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Alert save(Alert alert) {
        if (alert.getId() == null) {
            alert.assignId(sequence.incrementAndGet());
        }
        alerts.put(alert.getId(), copy(alert));
        return alert;
    }

    @Override
    public Optional<Alert> findById(long id) {
        return Optional.ofNullable(alerts.get(id)).map(InMemoryAlertRepository::copy);
    }

    @Override
    public List<Alert> findAll() {
        return alerts.values().stream().sorted(NEWEST_FIRST).map(InMemoryAlertRepository::copy).toList();
    }

    @Override
    public List<Alert> findByStatus(AlertStatus status) {
        return findAll().stream().filter(a -> a.getStatus() == status).toList();
    }

    private static Alert copy(Alert a) {
        return Alert.restore(a.getId(), a.getCageCode(), a.getGuineaPigId(), a.getLevel(), a.getType(),
                a.getMessage(), a.getStatus(), a.getCreatedAt(), a.getReviewedAt());
    }
}
