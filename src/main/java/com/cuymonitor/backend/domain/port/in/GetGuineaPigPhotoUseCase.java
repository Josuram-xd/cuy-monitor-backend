package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.GuineaPigPhoto;

public interface GetGuineaPigPhotoUseCase {
    /** Throws GuineaPigNotFoundException if the id is not an active guinea pig of that cage, or has no photo. */
    GuineaPigPhoto getPhoto(String cageCode, long guineaPigId);
}
