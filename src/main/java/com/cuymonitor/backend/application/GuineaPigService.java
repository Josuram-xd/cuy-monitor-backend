package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;

import java.util.List;

public class GuineaPigService implements ListGuineaPigsUseCase {

    private final CageRepository cageRepository;
    private final GuineaPigRepository guineaPigRepository;

    public GuineaPigService(CageRepository cageRepository, GuineaPigRepository guineaPigRepository) {
        this.cageRepository = cageRepository;
        this.guineaPigRepository = guineaPigRepository;
    }

    @Override
    public List<GuineaPig> listGuineaPigs(String cageCode) {
        requireCage(cageCode);
        return guineaPigRepository.findActiveByCage(cageCode);
    }

    private void requireCage(String cageCode) {
        if (cageRepository.findByCode(cageCode).isEmpty()) {
            throw new CageNotFoundException(cageCode);
        }
    }
}
