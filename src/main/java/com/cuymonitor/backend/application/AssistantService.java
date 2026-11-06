package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.exception.AssistantUnavailableException;
import com.cuymonitor.backend.domain.exception.TooManyAssistantQuestionsException;
import com.cuymonitor.backend.domain.model.Cage;
import com.cuymonitor.backend.domain.model.CageHealthSummary;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.assistant.AssistantReply;
import com.cuymonitor.backend.domain.model.assistant.ChatMessage;
import com.cuymonitor.backend.domain.model.assistant.ChatRole;
import com.cuymonitor.backend.domain.port.in.AskAssistantCommand;
import com.cuymonitor.backend.domain.port.in.AskAssistantUseCase;
import com.cuymonitor.backend.domain.port.out.AssistantModel;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The help assistant. It answers with what the breeder can already see in the dashboard (the state of their own
 * cages, read through the cage access proxy, so it never sees another account's cages) plus a guide of the app.
 * It does not decide or change anything: health is still decided only by the State and the Chain.
 */
public class AssistantService implements AskAssistantUseCase {

    /** At most this many cages go into the context; more would only make the call slower and dearer. */
    static final int MAX_CAGES_IN_CONTEXT = 5;
    static final int MAX_REPLY_LENGTH = 2000;

    private final AssistantModel model;
    private final CageOperations cages;
    private final Clock clock;
    private final int maxQuestions;
    private final Duration questionWindow;
    private final Map<UUID, Deque<Instant>> recentQuestions = new ConcurrentHashMap<>();

    public AssistantService(AssistantModel model, CageOperations cages, Clock clock, int maxQuestions,
                            Duration questionWindow) {
        this.model = Objects.requireNonNull(model, "model");
        this.cages = Objects.requireNonNull(cages, "cages");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (maxQuestions < 1) {
            throw new IllegalArgumentException("maxQuestions must be at least 1");
        }
        this.maxQuestions = maxQuestions;
        this.questionWindow = Objects.requireNonNull(questionWindow, "questionWindow");
    }

    @Override
    public AssistantReply ask(AskAssistantCommand command) {
        countQuestion(command.requesterId());
        String instructions = AssistantPrompt.instructions(command.language(), snapshot(command.requesterId()));
        String answer = model.reply(instructions, alternating(command.conversation()));
        if (answer == null || answer.isBlank()) {
            throw new AssistantUnavailableException("the assistant gave an empty answer");
        }
        String text = answer.strip();
        return new AssistantReply(text.length() > MAX_REPLY_LENGTH ? text.substring(0, MAX_REPLY_LENGTH) : text);
    }

    // a sliding window per account: every question is a paid model call
    private void countQuestion(UUID requesterId) {
        Instant now = clock.instant();
        Deque<Instant> asked = recentQuestions.computeIfAbsent(requesterId, id -> new ArrayDeque<>());
        synchronized (asked) {
            while (!asked.isEmpty() && !asked.peekFirst().isAfter(now.minus(questionWindow))) {
                asked.pollFirst();
            }
            if (asked.size() >= maxQuestions) {
                throw new TooManyAssistantQuestionsException();
            }
            asked.addLast(now);
        }
    }

    private List<CageSnapshot> snapshot(UUID requesterId) {
        List<CageSnapshot> snapshot = new ArrayList<>();
        for (Cage cage : cages.listCages(requesterId).stream().limit(MAX_CAGES_IN_CONTEXT).toList()) {
            CageHealthSummary health = cages.getHealth(requesterId, cage.code());
            List<GuineaPigSnapshot> guineaPigs = health.guineaPigs().stream()
                    .map(GuineaPigSnapshot::of)
                    .toList();
            snapshot.add(new CageSnapshot(cage.name(), health.status().name(), guineaPigs,
                    health.audio().status().name(), health.weight().lastGrams(), health.updatedAt()));
        }
        return snapshot;
    }

    /**
     * The model wants the conversation to start with the breeder and alternate roles. The dashboard sends what it
     * shows, which may start with the assistant's greeting or repeat a role after a failed answer: drop the
     * leading assistant messages and join consecutive messages of the same role.
     */
    static List<ChatMessage> alternating(List<ChatMessage> conversation) {
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage message : conversation) {
            if (result.isEmpty() && message.role() == ChatRole.ASSISTANT) {
                continue;
            }
            if (!result.isEmpty() && result.getLast().role() == message.role()) {
                ChatMessage previous = result.removeLast();
                result.add(new ChatMessage(message.role(), join(previous.text(), message.text())));
            } else {
                result.add(message);
            }
        }
        return result;
    }

    private static String join(String first, String second) {
        String joined = first + "\n" + second;
        return joined.length() > ChatMessage.MAX_TEXT_LENGTH ? joined.substring(joined.length()
                - ChatMessage.MAX_TEXT_LENGTH) : joined;
    }

    /** What the assistant may know about one cage: only what the dashboard already shows. */
    record CageSnapshot(String name, String status, List<GuineaPigSnapshot> guineaPigs, String audioStatus,
                        Double lastWeightGrams, Instant updatedAt) {
    }

    record GuineaPigSnapshot(String markColor, String status, Instant statusSince, String breed) {
        static GuineaPigSnapshot of(GuineaPig guineaPig) {
            String breed = guineaPig.getProfile() == null ? null : guineaPig.getProfile().breed();
            return new GuineaPigSnapshot(guineaPig.getMarkColor(), guineaPig.getStatus().name(),
                    guineaPig.getStatusSince(), breed);
        }
    }
}
