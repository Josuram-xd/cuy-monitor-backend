package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.model.AlertHistoryPage;
import com.cuymonitor.backend.domain.model.AlertHistoryFilter;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.CageWeight;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigHistory;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.TimeRange;
import com.cuymonitor.backend.domain.port.in.CreateCageCommand;
import com.cuymonitor.backend.domain.port.in.ListAlertsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import com.cuymonitor.backend.domain.port.in.UpdateCageCommand;

import java.util.stream.Collectors;
import java.util.Set;
import java.util.List;
import java.util.UUID;

/**
 * <b>Proxy pattern — RealSubject.</b> Does the actual work of every {@link CageOperations} call by delegating to the
 * use cases. It does <i>not</i> check who is asking: that is the job of
 * {@link com.cuymonitor.backend.domain.access.CageAccessProxy}, which is the only one that holds a reference to it.
 * The requester is used only where the operation itself needs it (an account's cages, the owner of a new cage).
 */
public class CageOperationsService implements CageOperations {

    private final CageUseCases useCases;

    public CageOperationsService(CageUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public List<Cage> listCages(UUID requesterId) {
        return useCases.listCages().list(requesterId);
    }

    @Override
    public Cage createCage(UUID requesterId, String name, String location) {
        return useCases.createCage().create(new CreateCageCommand(requesterId, name, location));
    }

    @Override
    public Cage getCage(UUID requesterId, String cageCode) {
        return useCases.getCage().get(cageCode);
    }

    @Override
    public Cage updateCage(UUID requesterId, String cageCode, String name, String location) {
        return useCases.updateCage().update(new UpdateCageCommand(cageCode, name, location));
    }

    @Override
    public void deleteCage(UUID requesterId, String cageCode) {
        useCases.deleteCage().delete(cageCode);
    }

    @Override
    public CageHealthSummary getHealth(UUID requesterId, String cageCode) {
        return useCases.getCageHealth().getHealth(cageCode);
    }

    @Override
    public List<GuineaPig> listGuineaPigs(UUID requesterId, String cageCode) {
        return useCases.listGuineaPigs().list(cageCode);
    }

    @Override
    public GuineaPig registerGuineaPig(UUID requesterId, String cageCode, String markColor, GuineaPigPhoto photo,
                                      GuineaPigProfile profile) {
        return useCases.registerGuineaPig().register(new RegisterGuineaPigCommand(cageCode, markColor, photo,
                profile == null ? GuineaPigProfile.EMPTY : profile));
    }

    @Override
    public GuineaPigPhoto getGuineaPigPhoto(UUID requesterId, String cageCode, long guineaPigId) {
        return useCases.getGuineaPigPhoto().getPhoto(cageCode, guineaPigId);
    }

    @Override
    public void deleteGuineaPig(UUID requesterId, String cageCode, long guineaPigId) {
        useCases.deleteGuineaPig().delete(cageCode, guineaPigId);
    }

    @Override
    public GuineaPigHistory getGuineaPigHistory(UUID requesterId, long guineaPigId, TimeRange range) {
        return useCases.getGuineaPigHistory().history(guineaPigId, range);
    }

    @Override
    public CageWeight getCageWeight(UUID requesterId, String cageCode, TimeRange range) {
        return useCases.getCageWeight().weight(cageCode, range);
    }

    @Override
    public List<Alert> listAlerts(UUID requesterId, String cageCode, AlertStatus status) {
        ListAlertsUseCase alerts = useCases.listAlerts();
        return cageCode == null ? alerts.list(status) : alerts.listForCage(cageCode, status);
    }

    @Override
    public Alert reviewAlert(UUID requesterId, long alertId) {
        return useCases.reviewAlert().markReviewed(alertId);
    }

    @Override
    public AlertHistoryPage getAlertHistory(UUID requesterId, AlertHistoryFilter filter) {
        Set<String> readable = filter.asksForCage()
                ? Set.of(filter.cageCode())
                : listCages(requesterId).stream().map(Cage::code).collect(Collectors.toSet());
        return useCases.getAlertHistory().history(filter.within(readable));
    }
}
