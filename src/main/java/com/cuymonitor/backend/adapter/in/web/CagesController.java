package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.CageRequest;
import com.cuymonitor.backend.adapter.in.web.dto.CageResponse;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.port.in.ConnectHardwareStationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/** The cages of the signed-in farmer. Every call goes through the cage access proxy. */
@RestController
@RequestMapping("/api/v1/cages")
public class CagesController {

    private final CageOperations cageOperations;
    private final ConnectHardwareStationUseCase station;

    public CagesController(CageOperations cageOperations, ConnectHardwareStationUseCase station) {
        this.cageOperations = cageOperations;
        this.station = station;
    }

    @GetMapping
    public List<CageResponse> list(Principal principal) {
        return cageOperations.listCages(Requester.idOf(principal)).stream().map(this::view).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CageResponse create(Principal principal, @Valid @RequestBody CageRequest request) {
        return view(cageOperations.createCage(Requester.idOf(principal), request.name(), request.location()));
    }

    @GetMapping("/{cageId}")
    public CageResponse get(Principal principal, @PathVariable String cageId) {
        return view(cageOperations.getCage(Requester.idOf(principal), cageId));
    }

    @PutMapping("/{cageId}")
    public CageResponse update(Principal principal, @PathVariable String cageId,
                               @Valid @RequestBody CageRequest request) {
        return view(cageOperations.updateCage(Requester.idOf(principal), cageId, request.name(), request.location()));
    }

    private CageResponse view(Cage cage) {
        return CageResponse.from(cage, station.connectedTo(cage.code()));
    }

    /** Soft delete: its guinea pigs, alerts and history are kept. */
    @DeleteMapping("/{cageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable String cageId) {
        cageOperations.deleteCage(Requester.idOf(principal), cageId);
    }
}
