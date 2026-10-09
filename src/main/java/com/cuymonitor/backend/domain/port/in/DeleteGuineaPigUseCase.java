package com.cuymonitor.backend.domain.port.in;

public interface DeleteGuineaPigUseCase {
    /**
     * Soft delete: the guinea pig stops showing and frees its mark color, but its events, alerts and history stay.
     *
     * @throws com.cuymonitor.backend.domain.exception.CageNotFoundException if the cage does not exist
     * @throws com.cuymonitor.backend.domain.exception.GuineaPigNotFoundException if it is not an active one of that cage
     */
    void delete(String cageCode, long guineaPigId);
}
