package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.GuineaPigNotFoundException;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigHistory;
import com.cuymonitor.backend.domain.model.TimeRange;
import com.cuymonitor.backend.domain.port.in.GetGuineaPigHistoryUseCase;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.StateTransitionRepository;
import org.springframework.transaction.annotation.Transactional;

/** The behavior history of a guinea pig for its detail page (contract 3.4). */
public class GuineaPigHistoryService implements GetGuineaPigHistoryUseCase {

    private final GuineaPigRepository guineaPigs;
    private final StateTransitionRepository transitions;
    private final EventRepository events;
    private final BaselineProfileRepository baselines;

    public GuineaPigHistoryService(GuineaPigRepository guineaPigs, StateTransitionRepository transitions,
                                   EventRepository events, BaselineProfileRepository baselines) {
        this.guineaPigs = guineaPigs;
        this.transitions = transitions;
        this.events = events;
        this.baselines = baselines;
    }

    @Override
    @Transactional(readOnly = true)
    public GuineaPigHistory history(long guineaPigId, TimeRange range) {
        // a deleted guinea pig keeps its history, so it is found even when not active
        GuineaPig pig = guineaPigs.findById(guineaPigId).orElseThrow(GuineaPigNotFoundException::new);
        var windows = events.findBehaviorBetween(pig.getId(), range.from(), range.to()).stream()
                .map(GuineaPigHistory.BehaviorWindow::of)
                .toList();
        BaselineProfile baseline = baselines.findByGuineaPigId(pig.getId())
                .orElseGet(() -> BaselineProfile.defaultFor(pig.getId()));
        return new GuineaPigHistory(pig.getId(), range,
                transitions.findByGuineaPigBetween(pig.getId(), range.from(), range.to()), windows, baseline);
    }
}
