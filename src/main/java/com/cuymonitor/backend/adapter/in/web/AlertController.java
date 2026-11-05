package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.AlertHistoryResponse;
import com.cuymonitor.backend.adapter.in.web.dto.AlertResponse;
import com.cuymonitor.backend.adapter.in.web.dto.UpdateAlertRequest;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Alerts of the account's cages. The cage access proxy filters and checks them. */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final CageOperations cageOperations;
    private final Clock clock;

    public AlertController(CageOperations cageOperations, Clock clock) {
        this.cageOperations = cageOperations;
        this.clock = clock;
    }

    /** cageId is optional: without it, the alerts of every cage of the account. */
    @GetMapping
    public List<AlertResponse> list(Principal principal, @RequestParam(required = false) AlertStatus status,
                                    @RequestParam(required = false) String cageId) {
        return cageOperations.listAlerts(Requester.idOf(principal), cageId, status).stream()
                .map(AlertResponse::from)
                .toList();
    }

    // every filter is optional; without from/to, the last 7 days. Pages of 20 by default, 100 at most
    @GetMapping("/history")
    public AlertHistoryResponse history(Principal principal,
                                        @RequestParam(required = false) String cageId,
                                        @RequestParam(required = false) Long guineaPigId,
                                        @RequestParam(required = false) HealthStatus level,
                                        @RequestParam(required = false) EventType type,
                                        @RequestParam(required = false) AlertStatus status,
                                        @RequestParam(required = false) Instant from,
                                        @RequestParam(required = false) Instant to,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        var params = new AlertHistoryParams(cageId, guineaPigId, level, type, status, from, to, page, size);
        return AlertHistoryResponse.from(
                cageOperations.getAlertHistory(Requester.idOf(principal), params.toFilter(clock)));
    }

    /** The only change allowed is OPEN to REVIEWED; there is no way back. */
    @PatchMapping("/{id}")
    public AlertResponse update(Principal principal, @PathVariable long id,
                                @Valid @RequestBody UpdateAlertRequest request) {
        if (request.status() != AlertStatus.REVIEWED) {
            throw new InvalidRequestException("only REVIEWED is accepted");
        }
        return AlertResponse.from(cageOperations.reviewAlert(Requester.idOf(principal), id));
    }
}
