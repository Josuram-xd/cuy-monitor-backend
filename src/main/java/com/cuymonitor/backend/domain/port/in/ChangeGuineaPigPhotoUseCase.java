package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.GuineaPigPhoto;

public interface ChangeGuineaPigPhotoUseCase {
    /**
     * Replaces the photo of an existing guinea pig.
     *
     * @throws com.cuymonitor.backend.domain.exception.CageNotFoundException if the cage does not exist
     * @throws com.cuymonitor.backend.domain.exception.GuineaPigNotFoundException if it is not an active one of that cage
     */
    void replacePhoto(String cageCode, long guineaPigId, GuineaPigPhoto photo);

    /** Same errors as {@link #replacePhoto}. Removing a photo nobody set is not an error. */
    void removePhoto(String cageCode, long guineaPigId);
}
