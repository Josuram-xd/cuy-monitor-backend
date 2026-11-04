package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.CageWeight;

import java.time.Instant;
import java.util.List;

/** Shape of GET /api/v1/cages/{cageId}/weight (docs/contracts/rest-api.md, 3.7). */
public record CageWeightResponse(String cageId, List<Reading> readings) {

    public record Reading(double grams, boolean stable, Instant measuredAt) {
    }

    public static CageWeightResponse from(CageWeight weight) {
        return new CageWeightResponse(weight.cageCode(), weight.readings().stream()
                .map(r -> new Reading(r.grams(), r.stable(), r.measuredAt()))
                .toList());
    }
}
