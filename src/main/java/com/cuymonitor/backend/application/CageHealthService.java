package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import com.cuymonitor.backend.domain.health.composite.CageAudioHealth;
import com.cuymonitor.backend.domain.health.composite.CageHealth;
import com.cuymonitor.backend.domain.health.composite.CageWeightHealth;
import com.cuymonitor.backend.domain.health.composite.GuineaPigHealth;
import com.cuymonitor.backend.domain.health.composite.HealthComponent;
import com.cuymonitor.backend.domain.health.state.GuineaPigHealthContext;
import com.cuymonitor.backend.domain.health.state.HealthStates;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.WeightReading;
import com.cuymonitor.backend.domain.port.in.GetCageHealthUseCase;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.CageRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import com.cuymonitor.backend.domain.port.out.GuineaPigRepository;
import com.cuymonitor.backend.domain.port.out.WeightReadingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds the cage summary from what is stored: the state of each guinea pig plus the open alerts of the
 * cage-level signals (audio, weight). The cage status is the worst of all its parts.
 */
public class CageHealthService implements GetCageHealthUseCase {

    private final CageRepository cages;
    private final GuineaPigRepository guineaPigs;
    private final AlertRepository alerts;
    private final EventRepository events;
    private final WeightReadingRepository weights;
    private final Clock clock;

    public CageHealthService(CageRepository cages, GuineaPigRepository guineaPigs, AlertRepository alerts,
                             EventRepository events, WeightReadingRepository weights, Clock clock) {
        this.cages = cages;
        this.guineaPigs = guineaPigs;
        this.alerts = alerts;
        this.events = events;
        this.weights = weights;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CageHealthSummary getHealth(String cageCode) {
        cages.findByCode(cageCode).orElseThrow(CageNotFoundException::new);

        List<GuineaPig> pigs = guineaPigs.findActiveByCage(cageCode);
        List<Alert> openAlerts = alerts.findByStatus(AlertStatus.OPEN).stream()
                .filter(alert -> alert.belongsTo(cageCode))
                .toList();

        var audio = new CageHealthSummary.Audio(
                worstOpenLevel(openAlerts, EventType.AUDIO),
                events.findLatest(cageCode, EventType.AUDIO).map(HealthEvent::occurredAt).orElse(null));
        Optional<WeightReading> lastWeight = weights.findLatest(cageCode);
        var weight = new CageHealthSummary.Weight(
                worstOpenLevel(openAlerts, EventType.WEIGHT),
                lastWeight.map(WeightReading::grams).orElse(null),
                lastWeight.map(WeightReading::measuredAt).orElse(null));

        HealthComponent cage = cageHealthTree(pigs, audio.status(), weight.status());
        return new CageHealthSummary(cageCode, cage.status(), pigs, audio, weight, clock.instant());
    }

    /**
     * <b>Composite pattern.</b> The cage is a composite of leaves (one per guinea pig, the audio and the scale);
     * asking the root for its status asks every part and keeps the worst.
     */
    private static HealthComponent cageHealthTree(List<GuineaPig> pigs, HealthStatus audio, HealthStatus weight) {
        List<HealthComponent> parts = new ArrayList<>();
        for (GuineaPig pig : pigs) {
            var context = new GuineaPigHealthContext(pig.getId(), HealthStates.of(pig.getStatus()));
            parts.add(new GuineaPigHealth(context));
        }
        parts.add(new CageAudioHealth(audio));
        parts.add(new CageWeightHealth(weight));
        return new CageHealth(parts);
    }

    // an open alert is what puts a cage-level signal out of NORMAL
    private static HealthStatus worstOpenLevel(List<Alert> openAlerts, EventType type) {
        HealthStatus worst = HealthStatus.NORMAL;
        for (Alert alert : openAlerts) {
            if (alert.getType() == type) {
                worst = HealthStatus.worst(worst, alert.getLevel());
            }
        }
        return worst;
    }
}
