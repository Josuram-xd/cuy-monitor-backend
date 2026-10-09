package com.cuymonitor.backend.adapter.out.persistence.memory;

import com.cuymonitor.backend.domain.model.WeightReading;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("memory")
public class InMemoryWeightReadingRepository implements WeightReadingRepository {

    private final List<WeightReading> readings = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public WeightReading save(WeightReading r) {
        WeightReading stored = new WeightReading(sequence.incrementAndGet(), r.cageCode(), r.grams(), r.stable(),
                r.measuredAt());
        readings.add(stored);
        return stored;
    }

    @Override
    public Optional<WeightReading> findLatest(String cageCode) {
        return readings.stream()
                .filter(r -> r.cageCode().equals(cageCode))
                .max(Comparator.comparing(WeightReading::measuredAt));
    }
}
