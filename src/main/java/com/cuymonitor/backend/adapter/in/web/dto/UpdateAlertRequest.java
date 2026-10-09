package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.AlertStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAlertRequest(@NotNull AlertStatus status) {
}
