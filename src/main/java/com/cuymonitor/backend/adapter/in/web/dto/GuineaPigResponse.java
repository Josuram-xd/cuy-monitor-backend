package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Instant;

// the last four are null for a guinea pig registered without them
public record GuineaPigResponse(long id, String name, String markColor, HealthStatus status, Instant statusSince,
                                String breed, String coatColor, Integer initialWeightGrams, String notes) {

    public static GuineaPigResponse from(GuineaPig guineaPig) {
        GuineaPigProfile profile = guineaPig.getProfile();
        return new GuineaPigResponse(guineaPig.getId(), guineaPig.getName(), guineaPig.getMarkColor(),
                guineaPig.getStatus(), guineaPig.getStatusSince(), profile.breed(), profile.coatColor(),
                profile.initialWeightGrams(), profile.notes());
    }
}
