package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.CageRequest;
import com.cuymonitor.backend.adapter.in.web.dto.CageResponse;
import com.cuymonitor.backend.domain.port.in.CreateCageCommand;
import com.cuymonitor.backend.domain.port.in.CreateCageUseCase;
import com.cuymonitor.backend.domain.port.in.DeleteCageUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageUseCase;
import com.cuymonitor.backend.domain.port.in.ListCagesUseCase;
import com.cuymonitor.backend.domain.port.in.UpdateCageCommand;
import com.cuymonitor.backend.domain.port.in.UpdateCageUseCase;
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

/** The cages of the signed-in farmer. */
@RestController
@RequestMapping("/api/v1/cages")
public class CagesController {

    private final ListCagesUseCase listCagesUseCase;
    private final GetCageUseCase getCageUseCase;
    private final CreateCageUseCase createCageUseCase;
    private final UpdateCageUseCase updateCageUseCase;
    private final DeleteCageUseCase deleteCageUseCase;

    public CagesController(ListCagesUseCase listCagesUseCase, GetCageUseCase getCageUseCase,
                           CreateCageUseCase createCageUseCase, UpdateCageUseCase updateCageUseCase,
                           DeleteCageUseCase deleteCageUseCase) {
        this.listCagesUseCase = listCagesUseCase;
        this.getCageUseCase = getCageUseCase;
        this.createCageUseCase = createCageUseCase;
        this.updateCageUseCase = updateCageUseCase;
        this.deleteCageUseCase = deleteCageUseCase;
    }

    @GetMapping
    public List<CageResponse> list(Principal principal) {
        return listCagesUseCase.list(accountId(principal)).stream().map(CageResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CageResponse create(Principal principal, @Valid @RequestBody CageRequest request) {
        return CageResponse.from(createCageUseCase.create(
                new CreateCageCommand(accountId(principal), request.name(), request.location())));
    }

    @GetMapping("/{cageId}")
    public CageResponse get(@PathVariable String cageId) {
        return CageResponse.from(getCageUseCase.get(cageId));
    }

    @PutMapping("/{cageId}")
    public CageResponse update(@PathVariable String cageId, @Valid @RequestBody CageRequest request) {
        return CageResponse.from(updateCageUseCase.update(
                new UpdateCageCommand(cageId, request.name(), request.location())));
    }

    /** Soft delete: its guinea pigs, alerts and history are kept. */
    @DeleteMapping("/{cageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String cageId) {
        deleteCageUseCase.delete(cageId);
    }

    // the JWT subject is the account id; it always comes from the session, never from the request
    static UUID accountId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
