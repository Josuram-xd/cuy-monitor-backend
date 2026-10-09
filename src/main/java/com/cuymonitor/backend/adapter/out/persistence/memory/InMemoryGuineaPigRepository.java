package com.cuymonitor.backend.adapter.out.persistence.memory;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryGuineaPigRepository implements GuineaPigRepository {

    private final Map<Long, GuineaPig> guineaPigs = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public GuineaPig save(GuineaPig guineaPig) {
        long id = guineaPig.getId() != null ? guineaPig.getId() : sequence.incrementAndGet();
        GuineaPig stored = copy(guineaPig, id);
        guineaPigs.put(id, stored);
        return copy(stored, id);
    }

    @Override
    public Optional<GuineaPig> findById(long id) {
        return Optional.ofNullable(guineaPigs.get(id)).map(g -> copy(g, id));
    }

    @Override
    public Optional<GuineaPig> findActiveByCageAndColor(String cageCode, MarkColor markColor) {
        return findActiveByCage(cageCode).stream().filter(g -> g.getMarkColor() == markColor).findFirst();
    }

    @Override
    public List<GuineaPig> findActiveByCage(String cageCode) {
        return guineaPigs.values().stream()
                .filter(g -> g.isActive() && g.getCageCode().equals(cageCode))
                .sorted(Comparator.comparing(GuineaPig::getId))
                .map(g -> copy(g, g.getId()))
                .toList();
    }

    private static GuineaPig copy(GuineaPig g, long id) {
        return GuineaPig.restore(id, g.getCageCode(), g.getName(), g.getMarkColor(), g.getStatus(),
                g.getStatusSince(), g.isActive(), g.getCreatedAt());
    }
}
