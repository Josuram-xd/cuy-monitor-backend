package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.Cage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/v1/cages and PUT /api/v1/cages/{cageId}. location is optional. */
public record CageRequest(
        @NotBlank @Size(max = Cage.MAX_NAME_LENGTH) String name,
        @Size(max = Cage.MAX_LOCATION_LENGTH) String location) {
}
