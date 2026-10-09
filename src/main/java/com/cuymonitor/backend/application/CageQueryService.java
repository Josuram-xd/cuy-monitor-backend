package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.health.composite.CageHealth;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.port.in.GetCageHealthUseCase;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;

import java.time.Clock;

public class CageQueryService implements GetCageHealthUseCase {

    private final CageRepository cageRepository;
    private final GuineaPigRepository guineaPigRepository;
    private final EventRepository eventRepository;
    private final WeightReadingRepository weightReadingRepository;
    private final Clock clock;

    public CageQueryService(CageRepository cageRepository, GuineaPigRepository guineaPigRepository,
                            EventRepository eventRepository, WeightReadingRepository weightReadingRepository,
                            Clock clock) {
        this.cageRepository = cageRepository;
        this.guineaPigRepository = guineaPigRepository;
        this.eventRepository = eventRepository;
        this.weightReadingRepository = weightReadingRepository;
        this.clock = clock;
    }

    @Override
    public CageHealth getCageHealth(String cageCode) {
        if (cageRepository.findByCode(cageCode).isEmpty()) {
            throw new CageNotFoundException(cageCode);
        }
        return CageHealth.of(cageCode,
                guineaPigRepository.findActiveByCage(cageCode),
                eventRepository.findLatest(cageCode, EventType.AUDIO),
                weightReadingRepository.findLatest(cageCode),
                clock.instant());
    }
}
