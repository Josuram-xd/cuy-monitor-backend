package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.in.CreateCageCommand;
import com.cuymonitor.backend.domain.port.in.CreateCageUseCase;
import com.cuymonitor.backend.domain.port.in.DeleteCageUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageUseCase;
import com.cuymonitor.backend.domain.port.in.ListCagesUseCase;
import com.cuymonitor.backend.domain.port.in.UpdateCageCommand;
import com.cuymonitor.backend.domain.port.in.UpdateCageUseCase;
import com.cuymonitor.backend.domain.port.out.CageCodeGenerator;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * The cages of a farmer: list, create, rename and delete (soft). GRASP Controller of the cage use cases: it
 * coordinates, while {@link Cage} validates its own name and location (Information Expert) and
 * {@link CageCodeGenerator} picks the codes (Pure Fabrication).
 */
public class CageService implements ListCagesUseCase, GetCageUseCase, CreateCageUseCase, UpdateCageUseCase,
        DeleteCageUseCase {

    private final CageRepository cages;
    private final CageCodeGenerator codes;
    private final Clock clock;

    public CageService(CageRepository cages, CageCodeGenerator codes, Clock clock) {
        this.cages = cages;
        this.codes = codes;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cage> list(UUID ownerId) {
        return cages.findActiveByOwner(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Cage get(String cageCode) {
        return cages.findByCode(cageCode).orElseThrow(CageNotFoundException::new);
    }

    @Override
    @Transactional
    public Cage create(CreateCageCommand command) {
        Cage cage = Cage.create(codes.newCode(), command.name(), command.location(), command.ownerId(),
                clock.instant());
        return cages.save(cage);
    }

    @Override
    @Transactional
    public Cage update(UpdateCageCommand command) {
        Cage renamed = get(command.cageCode()).rename(command.name(), command.location());
        return cages.save(renamed);
    }

    @Override
    @Transactional
    public void delete(String cageCode) {
        cages.save(get(cageCode).deactivate());
    }
}
