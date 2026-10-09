package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.MarkColor;

import java.util.List;
import java.util.Optional;

public interface GuineaPigRepository {
    GuineaPig save(GuineaPig guineaPig);
    Optional<GuineaPig> findById(long id);
    Optional<GuineaPig> findActiveByCageAndColor(String cageCode, MarkColor markColor);
    List<GuineaPig> findActiveByCage(String cageCode);
}
