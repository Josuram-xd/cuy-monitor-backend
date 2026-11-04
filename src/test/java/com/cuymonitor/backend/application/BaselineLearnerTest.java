package com.cuymonitor.backend.application;

import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryBaselineProfileRepository;
import com.cuymonitor.backend.adapter.out.persistence.memory.InMemoryGuineaPigRepository;
import com.cuymonitor.backend.application.fake.Photos;
import com.cuymonitor.backend.domain.model.AnomalyThresholds;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BaselineLearnerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T14:00:00Z");

    private final InMemoryBaselineProfileRepository baselines = new InMemoryBaselineProfileRepository();
    private final BaselineLearner learner = new BaselineLearner(baselines, AnomalyThresholds.defaults(), 60);
    private GuineaPig canela;

    @BeforeEach
    void setUp() {
        canela = new InMemoryGuineaPigRepository().save(GuineaPig.register("cage-1", "RED", Photos.png(), NOW));
    }

    private static BehaviorSignal window(double stillSeconds, int feederVisits, double probAnomaly) {
        return new BehaviorSignal("RED", 60, stillSeconds, feederVisits, 1, 0.2, probAnomaly, 0.9);
    }

    @Test
    void learnsFromANormalWindowOfAHealthyGuineaPig() {
        assertThat(learner.learnFrom(canela, window(12, 2, 0.1), NOW)).isTrue();

        var profile = baselines.findByGuineaPigId(canela.getId()).orElseThrow();
        assertThat(profile.sampleCount()).isEqualTo(1);
        assertThat(profile.avgStillSeconds()).isEqualTo(12);
    }

    @Test
    void neverLearnsFromAnAnomalousWindow() {
        assertThat(learner.learnFrom(canela, window(12, 2, 0.9), NOW)).isFalse();

        assertThat(baselines.findByGuineaPigId(canela.getId())).isEmpty();
    }

    @Test
    void neverLearnsWhileTheGuineaPigIsNotNormal() {
        canela.changeStatus(HealthStatus.OBSERVED, NOW);

        assertThat(learner.learnFrom(canela, window(12, 2, 0.1), NOW)).isFalse();

        assertThat(baselines.findByGuineaPigId(canela.getId())).isEmpty();
    }
}
