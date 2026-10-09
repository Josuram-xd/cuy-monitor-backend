package com.cuymonitor.backend.adapter.out.persistence.memory;

import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Profile("memory")
public class InMemoryCageRepository implements CageRepository {

    private final List<Cage> cages = List.of(new Cage(1L, "cage-1", "Jaula piloto"));

    @Override
    public Optional<Cage> findByCode(String code) {
        return cages.stream().filter(c -> c.code().equals(code)).findFirst();
    }
}
