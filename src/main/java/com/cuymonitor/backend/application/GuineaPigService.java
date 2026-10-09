package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.ColorAlreadyUsedException;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

public class GuineaPigService implements ListGuineaPigsUseCase, RegisterGuineaPigUseCase {

    private final CageRepository cages;
    private final GuineaPigRepository guineaPigs;
    private final Clock clock;

    public GuineaPigService(CageRepository cages, GuineaPigRepository guineaPigs, Clock clock) {
        this.cages = cages;
        this.guineaPigs = guineaPigs;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuineaPig> list(String cageCode) {
        requireCage(cageCode);
        return guineaPigs.findActiveByCage(cageCode);
    }

    @Override
    @Transactional
    public GuineaPig register(RegisterGuineaPigCommand command) {
        requireCage(command.cageCode());
        if (guineaPigs.findActiveByCageAndColor(command.cageCode(), command.markColor()).isPresent()) {
            throw new ColorAlreadyUsedException();
        }
        return guineaPigs.save(
                GuineaPig.register(command.cageCode(), command.name(), command.markColor(), command.profile(),
                        clock.instant()));
    }

    private void requireCage(String cageCode) {
        cages.findByCode(cageCode).orElseThrow(CageNotFoundException::new);
    }
}
