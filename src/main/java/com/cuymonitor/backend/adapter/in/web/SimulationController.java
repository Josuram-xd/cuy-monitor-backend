package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.exception.SimulationUnavailableException;
import com.cuymonitor.backend.domain.port.in.StartSimulationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/** The dashboard's "Simulación" button. The cage access proxy checks the owner, as for any cage call. */
@RestController
public class SimulationController {

    private final StartSimulationUseCase startSimulation;

    public SimulationController(StartSimulationUseCase startSimulation) {
        this.startSimulation = startSimulation;
    }

    @PostMapping("/api/v1/cages/{cageId}/simulation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void start(Principal principal, @PathVariable String cageId) {
        startSimulation.start(Requester.idOf(principal), cageId);
    }

    @ExceptionHandler(SimulationUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleUnavailable(SimulationUnavailableException ex) {
        if (ex.isNoGuineaPigs()) {
            return ResponseEntity.badRequest().body(ApiError.body(ApiError.BAD_REQUEST, ex.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.body(ApiError.SERVICE_UNAVAILABLE, ex.getMessage()));
    }
}
