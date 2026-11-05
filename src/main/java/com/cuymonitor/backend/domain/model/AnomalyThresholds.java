package com.cuymonitor.backend.domain.model;

/**
 * When a behavior window looks wrong compared with what is normal for that guinea pig (Task 11.4). They come
 * from application.yml through DomainConfig; the domain never reads the configuration itself.
 *
 * @param classifierThreshold from this probAnomaly on, the AI service's own verdict is enough
 * @param stillFactor         still this many times longer than usual, without going to the feeder
 * @param groupDistanceFactor this many times farther from the group than usual
 */
public record AnomalyThresholds(double classifierThreshold, double stillFactor, double groupDistanceFactor) {

    public AnomalyThresholds {
        if (classifierThreshold <= 0 || classifierThreshold > 1) {
            throw new IllegalArgumentException("classifierThreshold must be in (0, 1]");
        }
        if (stillFactor <= 1 || groupDistanceFactor <= 1) {
            throw new IllegalArgumentException("the factors must be greater than 1");
        }
    }

    /** The values the project started with. */
    public static AnomalyThresholds defaults() {
        return new AnomalyThresholds(0.7, 1.5, 2.0);
    }
}
