package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Instant;

// photoUrl is null for a guinea pig registered before the photo existed; the last four for one without profile
public record GuineaPigResponse(long id, String photoUrl, String markColor, HealthStatus status, Instant statusSince,
                                String breed, String coatColor, Integer initialWeightGrams, String notes) {

    public static GuineaPigResponse from(GuineaPig guineaPig) {
        GuineaPigProfile profile = guineaPig.getProfile();
        return new GuineaPigResponse(guineaPig.getId(), photoUrl(guineaPig), guineaPig.getMarkColor(),
                guineaPig.getStatus(), guineaPig.getStatusSince(), profile.breed(), profile.coatColor(),
                profile.initialWeightGrams(), profile.notes());
    }

    /** Where the bytes are served; the dashboard puts it straight in an img tag. */
    public static String photoUrl(GuineaPig guineaPig) {
        if (!guineaPig.hasPhoto()) {
            return null;
        }
        return "/api/v1/cages/" + guineaPig.getCageCode() + "/guinea-pigs/" + guineaPig.getId() + "/photo";
    }
}
