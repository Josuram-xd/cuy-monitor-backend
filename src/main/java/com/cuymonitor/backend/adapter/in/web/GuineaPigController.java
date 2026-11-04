package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.GuineaPigHistoryResponse;
import com.cuymonitor.backend.domain.access.CageOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;

/** One guinea pig, outside of its cage address. Goes through the cage access proxy like everything else. */
@RestController
@RequestMapping("/api/v1/guinea-pigs/{guineaPigId}")
public class GuineaPigController {

    private final CageOperations cageOperations;
    private final Clock clock;

    public GuineaPigController(CageOperations cageOperations, Clock clock) {
        this.cageOperations = cageOperations;
        this.clock = clock;
    }

    /** Defaults to the last 24 hours; at most 31 days. */
    @GetMapping("/history")
    public GuineaPigHistoryResponse history(Principal principal, @PathVariable long guineaPigId,
                                            @RequestParam(required = false) Instant from,
                                            @RequestParam(required = false) Instant to) {
        return GuineaPigHistoryResponse.from(cageOperations.getGuineaPigHistory(Requester.idOf(principal),
                guineaPigId, TimeRangeParams.of(from, to, clock)));
    }
}
