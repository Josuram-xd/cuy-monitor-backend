package com.cuymonitor.backend.adapter.out.persistence.memory;

import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

// nothing is learned yet (Task 11.1), so the chain falls back to BaselineProfile.defaultFor
@Component
public class InMemoryBaselineProfileRepository implements BaselineProfileRepository {

    @Override
    public Optional<BaselineProfile> findByGuineaPigId(long guineaPigId) {
        return Optional.empty();
    }
}
