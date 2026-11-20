package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.CageResponse;
import com.cuymonitor.backend.domain.port.in.GetOrCreateSimulationCageUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/** The dashboard's "Jaula de simulación" button. The account is the one in the session, as for every cage. */
@RestController
public class SimulationCageController {

    private final GetOrCreateSimulationCageUseCase simulationCage;

    public SimulationCageController(GetOrCreateSimulationCageUseCase simulationCage) {
        this.simulationCage = simulationCage;
    }

    @PostMapping("/api/v1/simulation-cage")
    public CageResponse getOrCreate(Principal principal) {
        // a simulation cage never has the real kit connected (see the equipment controller)
        return CageResponse.from(simulationCage.getOrCreate(Requester.idOf(principal)), false);
    }
}
