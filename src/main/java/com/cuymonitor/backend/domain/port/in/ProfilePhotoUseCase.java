package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.user.ProfilePhoto;

import java.util.UUID;

/** The breeder's own picture: set it, replace it, remove it, or hand it back to the browser. */
public interface ProfilePhotoUseCase {

    /** @throws com.cuymonitor.backend.domain.exception.UserNotFoundException if the account has no photo. */
    ProfilePhoto getPhoto(UUID userId);

    /** Replaces whatever was there. */
    void setPhoto(UUID userId, ProfilePhoto photo);

    /** Removing a photo nobody set is not an error: the account ends up the way it was asked to be. */
    void removePhoto(UUID userId);
}
