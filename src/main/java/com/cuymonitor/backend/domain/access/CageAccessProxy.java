package com.cuymonitor.backend.domain.access;

import com.cuymonitor.backend.domain.exception.AlertNotFoundException;
import com.cuymonitor.backend.domain.exception.CageAccessDeniedException;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.CageRepository;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <b>Proxy pattern — Proxy (protection proxy).</b> Implements the same {@link CageOperations} interface as the real
 * subject and keeps a reference to it. Before forwarding a call it checks that the account asking owns the cage:
 *
 * <ol>
 *   <li>look up the cage ({@code CageNotFoundException} if there is none),</li>
 *   <li>compare its owner with {@code requesterId} ({@code CageAccessDeniedException} if they differ),</li>
 *   <li>forward the same call, with the same arguments, to the real subject.</li>
 * </ol>
 *
 * Calls that are already limited to the requester (list or create their own cages) go straight through. Listing
 * every alert is filtered on the way back, keeping only the alerts of the requester's cages.
 */
public class CageAccessProxy implements CageOperations {

    private static final System.Logger LOG = System.getLogger(CageAccessProxy.class.getName());

    private final CageOperations realSubject;
    private final CageRepository cages;
    private final AlertRepository alerts;

    public CageAccessProxy(CageOperations realSubject, CageRepository cages, AlertRepository alerts) {
        this.realSubject = Objects.requireNonNull(realSubject, "realSubject");
        this.cages = Objects.requireNonNull(cages, "cages");
        this.alerts = Objects.requireNonNull(alerts, "alerts");
    }

    // ---- no check needed: these only ever touch the requester's own cages

    @Override
    public List<Cage> listCages(UUID requesterId) {
        return realSubject.listCages(requesterId);
    }

    @Override
    public Cage createCage(UUID requesterId, String name, String location) {
        return realSubject.createCage(requesterId, name, location);
    }

    // ---- check the owner of the cage, then forward

    @Override
    public Cage getCage(UUID requesterId, String cageCode) {
        checkOwner(requesterId, cageCode);
        return realSubject.getCage(requesterId, cageCode);
    }

    @Override
    public Cage updateCage(UUID requesterId, String cageCode, String name, String location) {
        checkOwner(requesterId, cageCode);
        return realSubject.updateCage(requesterId, cageCode, name, location);
    }

    @Override
    public void deleteCage(UUID requesterId, String cageCode) {
        checkOwner(requesterId, cageCode);
        realSubject.deleteCage(requesterId, cageCode);
    }

    @Override
    public CageHealthSummary getHealth(UUID requesterId, String cageCode) {
        checkOwner(requesterId, cageCode);
        return realSubject.getHealth(requesterId, cageCode);
    }

    @Override
    public List<GuineaPig> listGuineaPigs(UUID requesterId, String cageCode) {
        checkOwner(requesterId, cageCode);
        return realSubject.listGuineaPigs(requesterId, cageCode);
    }

    @Override
    public GuineaPig registerGuineaPig(UUID requesterId, String cageCode, String name, MarkColor markColor,
                                       GuineaPigProfile profile) {
        checkOwner(requesterId, cageCode);
        return realSubject.registerGuineaPig(requesterId, cageCode, name, markColor, profile);
    }

    @Override
    public void deleteGuineaPig(UUID requesterId, String cageCode, long guineaPigId) {
        checkOwner(requesterId, cageCode);
        realSubject.deleteGuineaPig(requesterId, cageCode, guineaPigId);
    }

    @Override
    public List<Alert> listAlerts(UUID requesterId, String cageCode, AlertStatus status) {
        if (cageCode != null) {
            checkOwner(requesterId, cageCode);
            return realSubject.listAlerts(requesterId, cageCode, status);
        }
        // every cage: forward, then keep only what belongs to the requester's cages
        Set<String> ownCages = cages.findActiveByOwner(requesterId).stream()
                .map(Cage::code)
                .collect(Collectors.toSet());
        return realSubject.listAlerts(requesterId, null, status).stream()
                .filter(alert -> ownCages.stream().anyMatch(alert::belongsTo))
                .toList();
    }

    @Override
    public Alert reviewAlert(UUID requesterId, long alertId) {
        // the alert says which cage it belongs to; the owner of that cage is the only one who can review it
        Alert alert = alerts.findById(alertId).orElseThrow(AlertNotFoundException::new);
        checkOwner(requesterId, alert.getCageCode());
        return realSubject.reviewAlert(requesterId, alertId);
    }

    // the access rule itself, in one place
    private void checkOwner(UUID requesterId, String cageCode) {
        Cage cage = cages.findByCode(cageCode).orElseThrow(CageNotFoundException::new);
        if (!cage.isOwnedBy(requesterId)) {
            LOG.log(System.Logger.Level.WARNING, "account {0} was refused access to cage {1}", requesterId, cageCode);
            throw new CageAccessDeniedException();
        }
    }
}
