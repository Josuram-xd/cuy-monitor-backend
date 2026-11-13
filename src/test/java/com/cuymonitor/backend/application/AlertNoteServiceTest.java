package com.cuymonitor.backend.application;

import com.cuymonitor.backend.application.fake.MutableClock;
import com.cuymonitor.backend.domain.exception.AssistantUnavailableException;
import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import com.cuymonitor.backend.domain.model.assistant.ChatMessage;
import com.cuymonitor.backend.domain.port.out.AlertObserver;
import com.cuymonitor.backend.domain.port.out.AlertRepository;
import com.cuymonitor.backend.domain.port.out.AssistantModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AlertNoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");

    private final AlertRepository alerts = mock(AlertRepository.class);
    private final RecordingObserver dashboard = new RecordingObserver();
    private final MutableClock clock = new MutableClock(NOW);
    // runs the task right here, so the test sees the result without waiting on a pool
    private final Executor sameThread = Runnable::run;

    private AlertNoteService serviceWith(AssistantModel model) {
        return new AlertNoteService(model, alerts, dashboard, clock, sameThread, "es");
    }

    private static Alert storedAlert(HealthStatus level) {
        return Alert.restore(7L, "cage-1", 3L, level, EventType.BEHAVIOR, "Quieto mucho tiempo",
                com.cuymonitor.backend.domain.model.AlertStatus.OPEN, NOW, null);
    }

    @Test
    void writesWhatTheModelAnsweredOnTheAlertAndTellsTheDashboard() {
        AssistantModel model = mock(AssistantModel.class);
        given(model.reply(any(), any())).willReturn("  Se movió muy poco esta última media hora.  ");
        given(alerts.findById(7L)).willReturn(Optional.of(storedAlert(HealthStatus.ALERT)));

        serviceWith(model).onAlert(storedAlert(HealthStatus.ALERT));

        verify(alerts).save(any(Alert.class));
        assertThat(dashboard.seen).hasSize(1);
        Alert sent = dashboard.seen.get(0);
        assertThat(sent.hasAiNote()).isTrue();
        // stored stripped, so the dashboard never shows the model's stray spaces
        assertThat(sent.getAiNote()).isEqualTo("Se movió muy poco esta última media hora.");
        assertThat(sent.getAiNoteAt()).isEqualTo(NOW);
    }

    @Test
    void theAlertSurvivesAModelThatIsOff() {
        AssistantModel model = mock(AssistantModel.class);
        willThrow(new AssistantUnavailableException("off")).given(model).reply(any(), any());
        given(alerts.findById(7L)).willReturn(Optional.of(storedAlert(HealthStatus.ALERT)));

        serviceWith(model).onAlert(storedAlert(HealthStatus.ALERT));

        verify(alerts, never()).save(any(Alert.class));
        assertThat(dashboard.seen).isEmpty();
    }

    @Test
    void anEmptyAnswerIsNotStored() {
        AssistantModel model = mock(AssistantModel.class);
        given(model.reply(any(), any())).willReturn("   ");
        given(alerts.findById(7L)).willReturn(Optional.of(storedAlert(HealthStatus.ALERT)));

        serviceWith(model).onAlert(storedAlert(HealthStatus.ALERT));

        verify(alerts, never()).save(any(Alert.class));
        assertThat(dashboard.seen).isEmpty();
    }

    @Test
    void anAlertWithNoIdIsNotEvenAskedAbout() {
        AssistantModel model = mock(AssistantModel.class);

        serviceWith(model).onAlert(Alert.forCage("cage-1", EventType.AUDIO, HealthStatus.ALERT, "Chillidos", NOW));

        verify(alerts, never()).findById(anyLong());
        verify(model, never()).reply(any(), any());
    }

    @Test
    void aCriticalAlertIsToldToTheModelAsMoreUrgentThanAnAlert() {
        Alert critical = storedAlert(HealthStatus.CRITICAL);

        String forCritical = AlertNotePrompt.question(critical);
        String forAlert = AlertNotePrompt.question(storedAlert(HealthStatus.ALERT));

        assertThat(forCritical).contains("CRITICAL").contains("over an hour");
        assertThat(forAlert).contains("ALERT").contains("half an hour");
        // the alert's own message is what the model is allowed to work from
        assertThat(forCritical).contains("Quieto mucho tiempo");
    }

    @Test
    void theNoteIsAskedForInTheConfiguredLanguage() {
        assertThat(AlertNotePrompt.instructions("es")).contains("Spanish (Latin American)");
        assertThat(AlertNotePrompt.instructions("en")).contains("English");
        // it must never ask the breeder anything: it writes the note and stops
        assertThat(AlertNotePrompt.instructions("es")).contains("write the note, nothing else");
    }

    @Test
    void aNoteLongerThanTheColumnIsCutInsteadOfBreakingTheInsert() {
        AssistantModel model = mock(AssistantModel.class);
        given(model.reply(any(), any())).willReturn("a".repeat(Alert.MAX_AI_NOTE_LENGTH + 500));
        given(alerts.findById(7L)).willReturn(Optional.of(storedAlert(HealthStatus.ALERT)));

        serviceWith(model).onAlert(storedAlert(HealthStatus.ALERT));

        assertThat(dashboard.seen.get(0).getAiNote()).hasSize(Alert.MAX_AI_NOTE_LENGTH);
    }

    @Test
    void theModelOnlyGetsOneUserMessage() {
        AssistantModel model = mock(AssistantModel.class);
        given(model.reply(any(), any())).willReturn("Revisa si come.");
        given(alerts.findById(7L)).willReturn(Optional.of(storedAlert(HealthStatus.ALERT)));

        serviceWith(model).onAlert(storedAlert(HealthStatus.ALERT));

        @SuppressWarnings("unchecked")
        var conversation = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(model).reply(any(), conversation.capture());
        assertThat((List<ChatMessage>) conversation.getValue()).hasSize(1);
    }

    private static final class RecordingObserver implements AlertObserver {
        private final List<Alert> seen = new ArrayList<>();

        @Override
        public void onAlert(Alert alert) {
            seen.add(alert);
        }
    }
}
