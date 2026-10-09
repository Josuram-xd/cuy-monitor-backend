package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.BaselineProfile;

import java.util.Optional;

public interface BaselineProfileRepository {
    Optional<BaselineProfile> findByGuineaPigId(long guineaPigId);
}
