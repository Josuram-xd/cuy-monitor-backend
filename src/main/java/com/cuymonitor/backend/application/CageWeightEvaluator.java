package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.health.weight.WeightTrendPolicy;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.notification.AlertPublisher;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;

import java.time.Instant;
import java.util.Objects;

/**
 * Task 12.2: after every stable reading, checks whether the cage is getting lighter and, if so, opens one WEIGHT
 * alert of the cage. While that alert is open no other one is opened: the breeder already knows, and reviewing it
 * lets the next drop warn again. The alert is what the weight leaf of the Composite ({@code CageWeightHealth})
 * reads, the same way the audio leaf reads the distress alerts.
 */
public class CageWeightEvaluator {

    // the breeder reads it in the dashboard, so it is in Spanish like the rest of the interface
    private static final String DROP_MESSAGE = "El peso de la jaula bajó un %d %% frente a los días anteriores";

    private final WeightReadingRepository weights;
    private final AlertRepository alerts;
    private final AlertPublisher alertPublisher;
    private final WeightTrendPolicy policy;

    public CageWeightEvaluator(WeightReadingRepository weights, AlertRepository alerts, AlertPublisher alertPublisher,
                               WeightTrendPolicy policy) {
        this.weights = Objects.requireNonNull(weights, "weights");
        this.alerts = Objects.requireNonNull(alerts, "alerts");
        this.alertPublisher = Objects.requireNonNull(alertPublisher, "alertPublisher");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public void evaluate(String cageCode, Instant now) {
        policy.drop(weights.findBetween(cageCode, policy.from(now), now), now).ifPresent(drop -> {
            boolean alreadyOpen = alerts.findByStatus(AlertStatus.OPEN).stream()
                    .anyMatch(alert -> alert.belongsTo(cageCode) && alert.getType() == EventType.WEIGHT);
            if (!alreadyOpen) {
                alertPublisher.publish(Alert.forCage(cageCode, EventType.WEIGHT, HealthStatus.ALERT,
                        DROP_MESSAGE.formatted(Math.round(drop * 100)), now));
            }
        });
    }
}
