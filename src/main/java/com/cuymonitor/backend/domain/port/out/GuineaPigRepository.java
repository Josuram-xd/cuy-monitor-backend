package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;

import java.util.List;
import java.util.Optional;

public interface GuineaPigRepository {
    /** Creates a guinea pig with its photo, which is written once and never changed afterwards. */
    GuineaPig register(GuineaPig guineaPig, GuineaPigPhoto photo);

    /** Saves a change of state of an existing guinea pig. It never touches the photo. */
    GuineaPig save(GuineaPig guineaPig);

    Optional<GuineaPig> findById(long id);
    Optional<GuineaPig> findActiveByCageAndColor(String cageCode, String markColor);
    List<GuineaPig> findActiveByCage(String cageCode);

    /** The bytes, read only here: the lists never carry them. Empty if the guinea pig has no photo. */
    Optional<GuineaPigPhoto> findPhoto(long guineaPigId);
}
