package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.MarkColorAlreadyUsedException;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;

import java.time.Clock;
import java.util.List;

public class GuineaPigService implements ListGuineaPigsUseCase, RegisterGuineaPigUseCase {

    private final CageRepository cageRepository;
    private final GuineaPigRepository guineaPigRepository;
    private final Clock clock;

    public GuineaPigService(CageRepository cageRepository, GuineaPigRepository guineaPigRepository, Clock clock) {
        this.cageRepository = cageRepository;
        this.guineaPigRepository = guineaPigRepository;
        this.clock = clock;
    }

    @Override
    public List<GuineaPig> listGuineaPigs(String cageCode) {
        requireCage(cageCode);
        return guineaPigRepository.findActiveByCage(cageCode);
    }

    @Override
    public GuineaPig register(RegisterGuineaPigCommand command) {
        requireCage(command.cageCode());
        if (guineaPigRepository.findActiveByCageAndColor(command.cageCode(), command.markColor()).isPresent()) {
            throw new MarkColorAlreadyUsedException(command.markColor());
        }
        return guineaPigRepository.save(
                GuineaPig.register(command.cageCode(), command.name(), command.markColor(), clock.instant()));
    }

    private void requireCage(String cageCode) {
        if (cageRepository.findByCode(cageCode).isEmpty()) {
            throw new CageNotFoundException(cageCode);
        }
    }
}
