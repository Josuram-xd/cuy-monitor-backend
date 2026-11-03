package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.port.in.CreateCageCommand;
import com.cuymonitor.backend.domain.port.in.CreateCageUseCase;
import com.cuymonitor.backend.domain.port.in.DeleteCageUseCase;
import com.cuymonitor.backend.domain.port.in.DeleteGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageHealthUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageUseCase;
import com.cuymonitor.backend.domain.port.in.ListAlertsUseCase;
import com.cuymonitor.backend.domain.port.in.ListCagesUseCase;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.in.ReviewAlertUseCase;
import com.cuymonitor.backend.domain.port.in.UpdateCageCommand;
import com.cuymonitor.backend.domain.port.in.UpdateCageUseCase;

import java.util.List;
import java.util.UUID;

/**
 * <b>Proxy pattern — RealSubject.</b> Does the actual work of every {@link CageOperations} call by delegating to the
 * use cases. It does <i>not</i> check who is asking: that is the job of
 * {@link com.cuymonitor.backend.domain.access.CageAccessProxy}, which is the only one that holds a reference to it.
 * The requester is used only where the operation itself needs it (an account's cages, the owner of a new cage).
 */
public class CageOperationsService implements CageOperations {

    private final ListCagesUseCase listCages;
    private final GetCageUseCase getCage;
    private final CreateCageUseCase createCage;
    private final UpdateCageUseCase updateCage;
    private final DeleteCageUseCase deleteCage;
    private final GetCageHealthUseCase getCageHealth;
    private final ListGuineaPigsUseCase listGuineaPigs;
    private final RegisterGuineaPigUseCase registerGuineaPig;
    private final DeleteGuineaPigUseCase deleteGuineaPig;
    private final ListAlertsUseCase listAlerts;
    private final ReviewAlertUseCase reviewAlert;

    public CageOperationsService(CageService cageService, CageHealthService cageHealthService,
                                 GuineaPigService guineaPigService, AlertService alertService) {
        this.listCages = cageService;
        this.getCage = cageService;
        this.createCage = cageService;
        this.updateCage = cageService;
        this.deleteCage = cageService;
        this.getCageHealth = cageHealthService;
        this.listGuineaPigs = guineaPigService;
        this.registerGuineaPig = guineaPigService;
        this.deleteGuineaPig = guineaPigService;
        this.listAlerts = alertService;
        this.reviewAlert = alertService;
    }

    @Override
    public List<Cage> listCages(UUID requesterId) {
        return listCages.list(requesterId);
    }

    @Override
    public Cage createCage(UUID requesterId, String name, String location) {
        return createCage.create(new CreateCageCommand(requesterId, name, location));
    }

    @Override
    public Cage getCage(UUID requesterId, String cageCode) {
        return getCage.get(cageCode);
    }

    @Override
    public Cage updateCage(UUID requesterId, String cageCode, String name, String location) {
        return updateCage.update(new UpdateCageCommand(cageCode, name, location));
    }

    @Override
    public void deleteCage(UUID requesterId, String cageCode) {
        deleteCage.delete(cageCode);
    }

    @Override
    public CageHealthSummary getHealth(UUID requesterId, String cageCode) {
        return getCageHealth.getHealth(cageCode);
    }

    @Override
    public List<GuineaPig> listGuineaPigs(UUID requesterId, String cageCode) {
        return listGuineaPigs.list(cageCode);
    }

    @Override
    public GuineaPig registerGuineaPig(UUID requesterId, String cageCode, String name, MarkColor markColor,
                                      GuineaPigProfile profile) {
        return registerGuineaPig.register(new RegisterGuineaPigCommand(cageCode, name, markColor,
                profile == null ? GuineaPigProfile.EMPTY : profile));
    }

    @Override
    public void deleteGuineaPig(UUID requesterId, String cageCode, long guineaPigId) {
        deleteGuineaPig.delete(cageCode, guineaPigId);
    }

    @Override
    public List<Alert> listAlerts(UUID requesterId, String cageCode, AlertStatus status) {
        List<Alert> all = listAlerts.list(status);
        return cageCode == null ? all : all.stream().filter(a -> a.getCageCode().equals(cageCode)).toList();
    }

    @Override
    public Alert reviewAlert(UUID requesterId, long alertId) {
        return reviewAlert.markReviewed(alertId);
    }
}
