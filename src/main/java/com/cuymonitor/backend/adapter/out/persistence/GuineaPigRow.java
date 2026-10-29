package com.cuymonitor.backend.adapter.out.persistence;

import com.cuymonitor.backend.domain.model.HealthStatus;

import java.time.Instant;

/**
 * A guinea pig without its photo bytes. The lists use it so a page of the dashboard does not drag a few
 * hundred KB of pictures per cuy out of the database; the bytes travel only through the photo endpoint.
 */
public record GuineaPigRow(Long id, String cageCode, String photoContentType, String markColor, HealthStatus status,
                           Instant statusSince, boolean active, Instant createdAt, String breed, String coatColor,
                           Integer initialWeightGrams, String notes) {
}
