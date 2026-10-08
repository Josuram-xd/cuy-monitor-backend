package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.health.composite.CageAudioHealth;
import com.cuymonitor.backend.domain.health.composite.CageHealth;
import com.cuymonitor.backend.domain.health.composite.CageWeightHealth;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.MarkColor;
import com.cuymonitor.backend.domain.model.WeightReading;

import java.time.Instant;
import java.util.List;

// shape of GET /api/v1/cages/{cageId}/health (docs/contracts/rest-api.md, 3.1)
public record CageHealthResponse(String cageId, HealthStatus status, List<GuineaPigStatus> guineaPigs,
                                 AudioStatus audio, WeightStatus weight, Instant updatedAt) {

    public record GuineaPigStatus(Long id, String name, MarkColor markColor, HealthStatus status) {
    }

    public record AudioStatus(HealthStatus status, Instant lastEventAt) {
    }

    public record WeightStatus(HealthStatus status, Double lastGrams, Instant lastMeasuredAt) {
    }

    public static CageHealthResponse from(CageHealth cage, Instant updatedAt) {
        List<GuineaPigStatus> pigs = cage.guineaPigs().stream().map(leaf -> {
            GuineaPig pig = leaf.guineaPig();
            return new GuineaPigStatus(pig.getId(), pig.getName(), pig.getMarkColor(), leaf.status());
        }).toList();

        CageAudioHealth audio = cage.audio().orElseThrow();
        CageWeightHealth weight = cage.weight().orElseThrow();
        WeightReading lastReading = weight.lastReading().orElse(null);

        return new CageHealthResponse(cage.cageCode(), cage.status(), pigs,
                new AudioStatus(audio.status(), audio.lastEventAt().orElse(null)),
                new WeightStatus(weight.status(), lastReading == null ? null : lastReading.grams(),
                        lastReading == null ? null : lastReading.measuredAt()),
                updatedAt);
    }
}
