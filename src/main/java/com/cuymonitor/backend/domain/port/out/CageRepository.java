package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.Cage;

import java.util.Optional;

public interface CageRepository {
    Optional<Cage> findByCode(String code);
}
