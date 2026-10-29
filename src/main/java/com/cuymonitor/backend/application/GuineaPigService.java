package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.exception.ColorAlreadyUsedException;
import com.cuymonitor.backend.domain.exception.GuineaPigNotFoundException;
import com.cuymonitor.backend.domain.exception.UnknownCatalogOptionException;
import com.cuymonitor.backend.domain.model.CatalogKind;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.port.in.DeleteGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.in.GetGuineaPigPhotoUseCase;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.CatalogOptionRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

public class GuineaPigService implements ListGuineaPigsUseCase, RegisterGuineaPigUseCase, DeleteGuineaPigUseCase,
        GetGuineaPigPhotoUseCase {

    private final CageRepository cages;
    private final GuineaPigRepository guineaPigs;
    private final CatalogOptionRepository catalogOptions;
    private final Clock clock;

    public GuineaPigService(CageRepository cages, GuineaPigRepository guineaPigs,
                            CatalogOptionRepository catalogOptions, Clock clock) {
        this.cages = cages;
        this.guineaPigs = guineaPigs;
        this.catalogOptions = catalogOptions;
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
        requireCatalogOption(CatalogKind.MARK_COLOR, command.markColor());
        requireCatalogOption(CatalogKind.COAT_COLOR, command.profile().coatColor());
        requireCatalogOption(CatalogKind.BREED, command.profile().breed());
        if (guineaPigs.findActiveByCageAndColor(command.cageCode(), command.markColor()).isPresent()) {
            throw new ColorAlreadyUsedException();
        }
        return guineaPigs.register(
                GuineaPig.register(command.cageCode(), command.markColor(), command.photo(), command.profile(),
                        clock.instant()),
                command.photo());
    }

    @Override
    @Transactional
    public void delete(String cageCode, long guineaPigId) {
        GuineaPig guineaPig = requireActiveGuineaPig(cageCode, guineaPigId);
        guineaPig.deactivate();
        guineaPigs.save(guineaPig);
    }

    @Override
    @Transactional(readOnly = true)
    public GuineaPigPhoto getPhoto(String cageCode, long guineaPigId) {
        requireActiveGuineaPig(cageCode, guineaPigId);
        return guineaPigs.findPhoto(guineaPigId).orElseThrow(GuineaPigNotFoundException::new);
    }

    private void requireCage(String cageCode) {
        cages.findByCode(cageCode).orElseThrow(CageNotFoundException::new);
    }

    // the id must belong to an active guinea pig of THIS cage: another cage's id is a 404, not a hit
    private GuineaPig requireActiveGuineaPig(String cageCode, long guineaPigId) {
        requireCage(cageCode);
        return guineaPigs.findById(guineaPigId)
                .filter(g -> g.isActive() && g.getCageCode().equals(cageCode))
                .orElseThrow(GuineaPigNotFoundException::new);
    }

    // the lists live in catalog_option, so a code that was retired there stops being accepted here
    private void requireCatalogOption(CatalogKind kind, String code) {
        if (code != null && !catalogOptions.existsActive(kind, code)) {
            throw new UnknownCatalogOptionException(kind, code);
        }
    }
}
