package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.port.in.ConnectHardwareStationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/cages/{cageId}/equipment")
public class HardwareStationController {
    private final CageOperations cages;
    private final ConnectHardwareStationUseCase station;

    public HardwareStationController(CageOperations cages, ConnectHardwareStationUseCase station) {
        this.cages = cages;
        this.station = station;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void connect(Principal principal, @PathVariable String cageId) {
        cages.getCage(Requester.idOf(principal), cageId);
        if (!station.connect(cageId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Equipment in use");
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void disconnect(Principal principal, @PathVariable String cageId) {
        cages.getCage(Requester.idOf(principal), cageId);
        station.disconnect(cageId);
    }
}
