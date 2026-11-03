package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.port.in.CreateCageUseCase;
import com.cuymonitor.backend.domain.port.in.DeleteCageUseCase;
import com.cuymonitor.backend.domain.port.in.DeleteGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageHealthUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageUseCase;
import com.cuymonitor.backend.domain.port.in.ListAlertsUseCase;
import com.cuymonitor.backend.domain.port.in.ListCagesUseCase;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.in.ReviewAlertUseCase;
import com.cuymonitor.backend.domain.port.in.UpdateCageUseCase;

import java.util.Objects;

/**
 * The input ports the real subject of the cage proxy works with, grouped so its constructor stays short. Only
 * interfaces (Dependency Inversion): the real subject never knows which service implements each one.
 */
public record CageUseCases(
        ListCagesUseCase listCages,
        GetCageUseCase getCage,
        CreateCageUseCase createCage,
        UpdateCageUseCase updateCage,
        DeleteCageUseCase deleteCage,
        GetCageHealthUseCase getCageHealth,
        ListGuineaPigsUseCase listGuineaPigs,
        RegisterGuineaPigUseCase registerGuineaPig,
        DeleteGuineaPigUseCase deleteGuineaPig,
        ListAlertsUseCase listAlerts,
        ReviewAlertUseCase reviewAlert) {

    public CageUseCases {
        Objects.requireNonNull(listCages, "listCages");
        Objects.requireNonNull(getCage, "getCage");
        Objects.requireNonNull(createCage, "createCage");
        Objects.requireNonNull(updateCage, "updateCage");
        Objects.requireNonNull(deleteCage, "deleteCage");
        Objects.requireNonNull(getCageHealth, "getCageHealth");
        Objects.requireNonNull(listGuineaPigs, "listGuineaPigs");
        Objects.requireNonNull(registerGuineaPig, "registerGuineaPig");
        Objects.requireNonNull(deleteGuineaPig, "deleteGuineaPig");
        Objects.requireNonNull(listAlerts, "listAlerts");
        Objects.requireNonNull(reviewAlert, "reviewAlert");
    }
}
