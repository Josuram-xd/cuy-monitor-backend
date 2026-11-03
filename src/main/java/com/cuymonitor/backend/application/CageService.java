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
import com.cuymonitor.backend.domain.port.out.CageRepository;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** The cages of a farmer: list, create, rename and delete (soft). */
public class CageService implements ListCagesUseCase, GetCageUseCase, CreateCageUseCase, UpdateCageUseCase,
        DeleteCageUseCase {

    private static final String CODE_PREFIX = "cage-";
    private static final int CODE_BYTES = 4;
    private static final int MAX_CODE_ATTEMPTS = 10;

    private final CageRepository cages;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public CageService(CageRepository cages, Clock clock) {
        this.cages = cages;
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
        return cages.save(Cage.create(newCode(), command.name(), command.location(), command.ownerId(),
                clock.instant()));
    }

    @Override
    @Transactional
    public Cage update(UpdateCageCommand command) {
        return cages.save(get(command.cageCode()).rename(command.name(), command.location()));
    }

    @Override
    @Transactional
    public void delete(String cageCode) {
        cages.save(get(cageCode).deactivate());
    }

    // "cage-" + 8 hex characters: short enough to type on the camera and the scale, random so it is not guessable
    private String newCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            byte[] bytes = new byte[CODE_BYTES];
            random.nextBytes(bytes);
            String code = CODE_PREFIX + HexFormat.of().formatHex(bytes);
            if (!cages.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("could not pick a free cage code");
    }
}
