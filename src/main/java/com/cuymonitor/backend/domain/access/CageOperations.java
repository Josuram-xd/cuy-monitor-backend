package com.cuymonitor.backend.domain.access;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;

import java.util.List;
import java.util.UUID;

/**
 * <b>Proxy pattern — Subject.</b> Everything an account can do with its cages. Implemented by the real subject
 * ({@code CageOperationsService}) and by the protection proxy ({@link CageAccessProxy}); the client can't tell them
 * apart. {@code requesterId} is the account that asks, taken from its session.
 */
public interface CageOperations {

    // ---- the cages themselves

    List<Cage> listCages(UUID requesterId);

    Cage createCage(UUID requesterId, String name, String location);

    Cage getCage(UUID requesterId, String cageCode);

    Cage updateCage(UUID requesterId, String cageCode, String name, String location);

    void deleteCage(UUID requesterId, String cageCode);

    // ---- what is inside a cage

    CageHealthSummary getHealth(UUID requesterId, String cageCode);

    List<GuineaPig> listGuineaPigs(UUID requesterId, String cageCode);

    /** markColor is a code of the catalog of mark colors ("RED"); the photo is how the breeder recognizes it. */
    GuineaPig registerGuineaPig(UUID requesterId, String cageCode, String markColor, GuineaPigPhoto photo,
                                GuineaPigProfile profile);

    GuineaPigPhoto getGuineaPigPhoto(UUID requesterId, String cageCode, long guineaPigId);

    void deleteGuineaPig(UUID requesterId, String cageCode, long guineaPigId);

    // ---- alerts

    /** Newest first. A null cageCode means every cage the requester can see; a null status means every status. */
    List<Alert> listAlerts(UUID requesterId, String cageCode, AlertStatus status);

    Alert reviewAlert(UUID requesterId, long alertId);
}
