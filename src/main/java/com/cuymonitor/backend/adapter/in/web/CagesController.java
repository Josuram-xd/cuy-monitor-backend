package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.CageRequest;
import com.cuymonitor.backend.adapter.in.web.dto.CageResponse;
import com.cuymonitor.backend.domain.access.CageOperations;
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
import java.util.UUID;

/** The cages of the signed-in farmer. Every call goes through the cage access proxy. */
@RestController
@RequestMapping("/api/v1/cages")
public class CagesController {

    private final CageOperations cageOperations;

    public CagesController(CageOperations cageOperations) {
        this.cageOperations = cageOperations;
    }

    @GetMapping
    public List<CageResponse> list(Principal principal) {
        return cageOperations.listCages(accountId(principal)).stream().map(CageResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CageResponse create(Principal principal, @Valid @RequestBody CageRequest request) {
        return CageResponse.from(cageOperations.createCage(accountId(principal), request.name(), request.location()));
    }

    @GetMapping("/{cageId}")
    public CageResponse get(Principal principal, @PathVariable String cageId) {
        return CageResponse.from(cageOperations.getCage(accountId(principal), cageId));
    }

    @PutMapping("/{cageId}")
    public CageResponse update(Principal principal, @PathVariable String cageId,
                               @Valid @RequestBody CageRequest request) {
        return CageResponse.from(cageOperations.updateCage(accountId(principal), cageId, request.name(),
                request.location()));
    }

    /** Soft delete: its guinea pigs, alerts and history are kept. */
    @DeleteMapping("/{cageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable String cageId) {
        cageOperations.deleteCage(accountId(principal), cageId);
    }

    // the JWT subject is the account id; it always comes from the session, never from the request
    static UUID accountId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
