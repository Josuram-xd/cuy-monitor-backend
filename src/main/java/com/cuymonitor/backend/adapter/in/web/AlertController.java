package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.AlertResponse;
import com.cuymonitor.backend.adapter.in.web.dto.UpdateAlertRequest;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.port.in.ListAlertsUseCase;
import com.cuymonitor.backend.domain.port.in.ReviewAlertUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final ListAlertsUseCase listAlertsUseCase;
    private final ReviewAlertUseCase reviewAlertUseCase;

    public AlertController(ListAlertsUseCase listAlertsUseCase, ReviewAlertUseCase reviewAlertUseCase) {
        this.listAlertsUseCase = listAlertsUseCase;
        this.reviewAlertUseCase = reviewAlertUseCase;
    }

    @GetMapping
    public List<AlertResponse> list(@RequestParam(required = false) AlertStatus status) {
        return listAlertsUseCase.list(status).stream().map(AlertResponse::from).toList();
    }

    /** The only change allowed is OPEN to REVIEWED; there is no way back. */
    @PatchMapping("/{id}")
    public AlertResponse update(@PathVariable long id, @Valid @RequestBody UpdateAlertRequest request) {
        if (request.status() != AlertStatus.REVIEWED) {
            throw new InvalidRequestException("only REVIEWED is accepted");
        }
        return AlertResponse.from(reviewAlertUseCase.markReviewed(id));
    }
}
