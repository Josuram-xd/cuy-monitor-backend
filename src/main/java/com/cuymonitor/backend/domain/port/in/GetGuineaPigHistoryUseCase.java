package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.GuineaPigHistory;
import com.cuymonitor.backend.domain.model.TimeRange;

public interface GetGuineaPigHistoryUseCase {
    /** @throws com.cuymonitor.backend.domain.exception.GuineaPigNotFoundException if there is no such guinea pig */
    GuineaPigHistory history(long guineaPigId, TimeRange range);
}
