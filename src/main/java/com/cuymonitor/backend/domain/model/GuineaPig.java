package com.cuymonitor.backend.domain.model;

import java.time.Instant;
import java.util.Objects;

public class GuineaPig {

    private final Long id;
    private final String cageCode;
    private final String photoContentType;
    private final String markColor;
    private HealthStatus status;
    private Instant statusSince;
    private boolean active;
    private final Instant createdAt;
    private final GuineaPigProfile profile;

    private GuineaPig(Long id, String cageCode, String photoContentType, String markColor, HealthStatus status,
                      Instant statusSince, boolean active, Instant createdAt, GuineaPigProfile profile) {
        this.id = id;
        this.cageCode = Objects.requireNonNull(cageCode, "cageCode");
        this.photoContentType = photoContentType;
        this.markColor = requireMarkColor(markColor);
        this.status = Objects.requireNonNull(status, "status");
        this.statusSince = Objects.requireNonNull(statusSince, "statusSince");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.profile = profile == null ? GuineaPigProfile.EMPTY : profile;
    }

    public static GuineaPig register(String cageCode, String markColor, GuineaPigPhoto photo, Instant now) {
        return register(cageCode, markColor, photo, GuineaPigProfile.EMPTY, now);
    }

    public static GuineaPig register(String cageCode, String markColor, GuineaPigPhoto photo,
                                     GuineaPigProfile profile, Instant now) {
        Objects.requireNonNull(photo, "photo");
        return new GuineaPig(null, cageCode, photo.contentType(), markColor, HealthStatus.NORMAL, now, true, now,
                profile);
    }

    /** photoContentType is null for a guinea pig registered before the photo existed. */
    public static GuineaPig restore(Long id, String cageCode, String photoContentType, String markColor,
                                    HealthStatus status, Instant statusSince, boolean active, Instant createdAt) {
        return restore(id, cageCode, photoContentType, markColor, status, statusSince, active, createdAt,
                GuineaPigProfile.EMPTY);
    }

    public static GuineaPig restore(Long id, String cageCode, String photoContentType, String markColor,
                                    HealthStatus status, Instant statusSince, boolean active, Instant createdAt,
                                    GuineaPigProfile profile) {
        return new GuineaPig(id, cageCode, photoContentType, markColor, status, statusSince, active, createdAt,
                profile);
    }

    /** Soft delete: it keeps its history but is no longer an active guinea pig of the cage. */
    public void deactivate() {
        active = false;
    }

    public void changeStatus(HealthStatus newStatus, Instant now) {
        Objects.requireNonNull(newStatus, "newStatus");
        if (newStatus != status) {
            status = newStatus;
            statusSince = Objects.requireNonNull(now, "now");
        }
    }

    public Long getId() {
        return id;
    }

    public String getCageCode() {
        return cageCode;
    }

    public boolean hasPhoto() {
        return photoContentType != null;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public String getMarkColor() {
        return markColor;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public Instant getStatusSince() {
        return statusSince;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public GuineaPigProfile getProfile() {
        return profile;
    }

    // a MARK_COLOR code of catalog_option; that the row exists is checked by GuineaPigService
    private static String requireMarkColor(String markColor) {
        String code = CatalogOption.normalize(markColor);
        if (code == null) {
            throw new IllegalArgumentException("markColor is required");
        }
        if (code.length() > CatalogOption.MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "markColor must have at most " + CatalogOption.MAX_CODE_LENGTH + " characters");
        }
        return code;
    }
}
