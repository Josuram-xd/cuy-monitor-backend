package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.Cage;

import java.time.Instant;

/** A cage as the dashboard sees it (docs/contracts/rest-api.md, 3.0). cageId is the public code. */
public record CageResponse(String cageId, String name, String location, Instant createdAt) {

    public static CageResponse from(Cage cage) {
        return new CageResponse(cage.code(), cage.name(), cage.location(), cage.createdAt());
    }
}
