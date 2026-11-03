package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.AlertResponse;
import com.cuymonitor.backend.adapter.in.web.dto.UpdateAlertRequest;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.model.AlertStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

import static com.cuymonitor.backend.adapter.in.web.CagesController.accountId;

/** Alerts of the account's cages. The cage access proxy filters and checks them. */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final CageOperations cageOperations;

    public AlertController(CageOperations cageOperations) {
        this.cageOperations = cageOperations;
    }

    /** cageId is optional: without it, the alerts of every cage of the account. */
    @GetMapping
    public List<AlertResponse> list(Principal principal, @RequestParam(required = false) AlertStatus status,
                                    @RequestParam(required = false) String cageId) {
        return cageOperations.listAlerts(accountId(principal), cageId, status).stream()
                .map(AlertResponse::from)
                .toList();
    }

    /** The only change allowed is OPEN to REVIEWED; there is no way back. */
    @PatchMapping("/{id}")
    public AlertResponse update(Principal principal, @PathVariable long id,
                                @Valid @RequestBody UpdateAlertRequest request) {
        if (request.status() != AlertStatus.REVIEWED) {
            throw new InvalidRequestException("only REVIEWED is accepted");
        }
        return AlertResponse.from(cageOperations.reviewAlert(accountId(principal), id));
    }
}
