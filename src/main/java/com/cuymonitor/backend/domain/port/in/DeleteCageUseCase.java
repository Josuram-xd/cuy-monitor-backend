package com.cuymonitor.backend.domain.port.in;

public interface DeleteCageUseCase {
    /**
     * Soft delete: the cage stops showing and stops receiving events; its guinea pigs, alerts and history stay.
     *
     * @throws com.cuymonitor.backend.domain.exception.CageNotFoundException if there is no active cage with that code
     */
    void delete(String cageCode);
}
