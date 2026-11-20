package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.exception.AssistantUnavailableException;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AnomalyThresholds;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.assistant.ChatMessage;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.AssistantModel;
import com.cuymonitor.backend.domain.port.out.BaselineProfileRepository;
import com.cuymonitor.backend.domain.port.out.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <b>Observer pattern — subscriber.</b> Makes the assistant react to an alert by itself: until now it only
 * answered when the breeder typed a question, so the explanation of an alert was never there when it was needed.
 *
 * <p>Asking the model takes seconds, so this subscriber runs synchronously: wrap it in
 * {@link com.cuymonitor.backend.domain.notification.AsyncAlertObserver} to take it off the ingestion request, and the
 * note then arrives a moment after the alert. A model that is off or throttled costs the alert nothing: it is
 * stored, sent and shown without a note.
 */
public final class AlertNoteService implements AlertObserver {

    private static final Logger log = LoggerFactory.getLogger(AlertNoteService.class);

    private final AssistantModel model;
    private final AlertRepository alerts;
    private final AlertObserver dashboard;
    private final Clock clock;
    private final String language;
    // null: the note is written from the alert's message only
    private final EventRepository events;
    private final BaselineProfileRepository baselines;
    private final Supplier<AnomalyThresholds> thresholds;

    public AlertNoteService(AssistantModel model, AlertRepository alerts, AlertObserver dashboard, Clock clock,
                            String language) {
        this(model, alerts, dashboard, clock, language, null, null, AnomalyThresholds::defaults);
    }

    public AlertNoteService(AssistantModel model, AlertRepository alerts, AlertObserver dashboard, Clock clock,
                            String language, EventRepository events, BaselineProfileRepository baselines,
                            Supplier<AnomalyThresholds> thresholds) {
        this.events = events;
        this.baselines = baselines;
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds");
        this.model = Objects.requireNonNull(model, "model");
        this.alerts = Objects.requireNonNull(alerts, "alerts");
        this.dashboard = Objects.requireNonNull(dashboard, "dashboard");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.language = Objects.requireNonNull(language, "language");
    }

    @Override
    public void onAlert(Alert alert) {
        Long id = Objects.requireNonNull(alert, "alert").getId();
        if (id == null) {
            // the database subscriber runs before this one, so this only happens if that one failed
            log.warn("alert with no id yet: no note will be written");
            return;
        }
        write(id);
    }

    /**
     * Reads the alert back instead of keeping the one that was published: that object belongs to the request
     * thread, and by now its row is the one the dashboard is showing.
     */
    private void write(long alertId) {
        try {
            Alert stored = alerts.findById(alertId).orElse(null);
            if (stored == null) {
                log.warn("alert {} is gone: no note will be written", alertId);
                return;
            }
            String note = model.reply(
                    AlertNotePrompt.instructions(language),
                    List.of(ChatMessage.user(AlertNotePrompt.question(stored, measurementsOf(stored)))));
            if (note == null || note.isBlank()) {
                log.warn("the assistant answered nothing about alert {}", alertId);
                return;
            }
            Instant now = clock.instant();
            stored.attachAiNote(note, now);
            alerts.save(stored);
            dashboard.onAlert(stored);
            log.info("note written for alert {}", alertId);
        } catch (AssistantUnavailableException ex) {
            // expected: the alert is already stored and shown, it just has no note
            log.warn("the assistant can't write about alert {} now: {}", alertId, ex.getMessage());
        } catch (RuntimeException ex) {
            // this runs on a pool thread: nothing above would ever see the failure
            log.error("could not write the note for alert {}", alertId, ex);
        }
    }

    /** What the camera measured on the guinea pig of a behavior alert, for the model; empty when there is nothing. */
    private String measurementsOf(Alert alert) {
        if (events == null || alert.getGuineaPigId() == null || alert.getType() != EventType.BEHAVIOR) {
            return "";
        }
        long guineaPigId = alert.getGuineaPigId();
        List<BehaviorSignal> windows = events.findLatestBehavior(guineaPigId, AlertNotePrompt.WINDOWS).stream()
                .map(HealthEvent::signal)
                .filter(BehaviorSignal.class::isInstance)
                .map(BehaviorSignal.class::cast)
                .toList();
        BaselineProfile baseline = (baselines == null ? java.util.Optional.<BaselineProfile>empty()
                : baselines.findByGuineaPigId(guineaPigId))
                .orElseGet(() -> BaselineProfile.defaultFor(guineaPigId));
        return AlertNotePrompt.measurements(windows, baseline, thresholds.get());
    }
}
