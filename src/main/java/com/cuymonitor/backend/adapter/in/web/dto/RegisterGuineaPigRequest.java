package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.MarkColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterGuineaPigRequest(@NotBlank @Size(max = 100) String name, @NotNull MarkColor markColor) {
}
