package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.StateTransition;

public interface StateTransitionRepository {
    StateTransition save(StateTransition transition);
}
