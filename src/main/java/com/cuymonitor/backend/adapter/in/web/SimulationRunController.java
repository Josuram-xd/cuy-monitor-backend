package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.port.in.ControlSimulationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * How far the video run of a cage got, and stopping it. Only the owner of the cage gets through: the cage access
 * proxy checks it before the AI service is asked.
 */
@RestController
@RequestMapping("/api/v1/cages/{cageId}/simulation")
public class SimulationRunController {

    private final CageOperations cages;
    private final ControlSimulationUseCase simulation;

    public SimulationRunController(CageOperations cages, ControlSimulationUseCase simulation) {
        this.cages = cages;
        this.simulation = simulation;
    }

    // the AI service's JSON goes out as it is: the dashboard reads running, framesSent and credit from it
    @GetMapping
    public ResponseEntity<String> status(Principal principal, @PathVariable String cageId) {
        cages.getCage(Requester.idOf(principal), cageId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(simulation.status(cageId));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(Principal principal, @PathVariable String cageId) {
        cages.getCage(Requester.idOf(principal), cageId);
        simulation.stop(cageId);
    }
}
