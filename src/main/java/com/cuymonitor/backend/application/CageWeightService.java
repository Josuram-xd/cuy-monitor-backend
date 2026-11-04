package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.model.CageWeight;
import com.cuymonitor.backend.domain.model.TimeRange;
import com.cuymonitor.backend.domain.port.in.GetCageWeightUseCase;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;
import org.springframework.transaction.annotation.Transactional;

/** The scale readings of a cage for its weight chart (contract 3.7). */
public class CageWeightService implements GetCageWeightUseCase {

    private final CageRepository cages;
    private final WeightReadingRepository weights;

    public CageWeightService(CageRepository cages, WeightReadingRepository weights) {
        this.cages = cages;
        this.weights = weights;
    }

    @Override
    @Transactional(readOnly = true)
    public CageWeight weight(String cageCode, TimeRange range) {
        cages.findByCode(cageCode).orElseThrow(CageNotFoundException::new);
        return new CageWeight(cageCode, range, weights.findBetween(cageCode, range.from(), range.to()));
    }
}
