package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.AlertResponse;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.port.in.ListAlertsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final ListAlertsUseCase listAlertsUseCase;

    public AlertController(ListAlertsUseCase listAlertsUseCase) {
        this.listAlertsUseCase = listAlertsUseCase;
    }

    @GetMapping
    public List<AlertResponse> list(@RequestParam(required = false) AlertStatus status) {
        return listAlertsUseCase.listAlerts(Optional.ofNullable(status)).stream().map(AlertResponse::from).toList();
    }
}
