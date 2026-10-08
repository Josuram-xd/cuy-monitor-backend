package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.CageHealthResponse;
import com.cuymonitor.backend.domain.port.in.GetCageHealthUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequestMapping("/api/v1/cages")
public class CageController {

    private final GetCageHealthUseCase getCageHealthUseCase;
    private final Clock clock;

    public CageController(GetCageHealthUseCase getCageHealthUseCase, Clock clock) {
        this.getCageHealthUseCase = getCageHealthUseCase;
        this.clock = clock;
    }

    @GetMapping("/{cageId}/health")
    public CageHealthResponse health(@PathVariable String cageId) {
        return CageHealthResponse.from(getCageHealthUseCase.getCageHealth(cageId), clock.instant());
    }
}
