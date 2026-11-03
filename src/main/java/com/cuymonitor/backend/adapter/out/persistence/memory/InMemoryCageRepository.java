package com.cuymonitor.backend.adapter.out.persistence.memory;

import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("memory")
public class InMemoryCageRepository implements CageRepository {

    private final Map<String, Cage> cages = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong(1);

    public InMemoryCageRepository() {
        // the pilot cage, without owner, like a database where V12 found no account
        save(new Cage(null, "cage-1", "Jaula piloto"));
    }

    @Override
    public Optional<Cage> findByCode(String code) {
        return Optional.ofNullable(cages.get(code)).filter(Cage::active);
    }

    @Override
    public List<Cage> findActiveByOwner(UUID ownerId) {
        return cages.values().stream()
                .filter(cage -> cage.active() && cage.isOwnedBy(ownerId))
                .sorted(Comparator.comparing(Cage::id))
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return cages.containsKey(code);
    }

    @Override
    public Cage save(Cage cage) {
        Cage stored = cage.id() != null ? cage : new Cage(ids.getAndIncrement(), cage.code(), cage.name(),
                cage.location(), cage.ownerId(), cage.active(), cage.createdAt());
        cages.put(stored.code(), stored);
        return stored;
    }
}
