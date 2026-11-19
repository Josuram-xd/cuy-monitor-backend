package com.cuymonitor.backend.application.admin;

import com.cuymonitor.backend.domain.model.AdminAction;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * <b>Command pattern — Invoker.</b> Runs any {@link AdminCommand} and writes down who ran it and what it did. It
 * never looks inside a command: adding a new admin action means writing a new command class, not touching this.
 *
 * <p>Only the commands that went through are written down; one a rule refused changed nothing, so it is not
 * history.
 */
public final class AdminCommandInvoker {

    // shortcut: kept in memory, lost on restart; move it to a table if the history has to survive a deploy
    public static final int MAX_HISTORY = 100;

    private final Clock clock;
    private final Deque<AdminAction> history = new ArrayDeque<>();

    public AdminCommandInvoker(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public <R> R run(UUID adminId, AdminCommand<R> command) {
        Objects.requireNonNull(adminId, "adminId");
        Objects.requireNonNull(command, "command");
        R result = command.execute();
        record(new AdminAction(clock.instant(), adminId, command.description()));
        return result;
    }

    /** The changes that went through, newest first. */
    public synchronized List<AdminAction> history() {
        return List.copyOf(history);
    }

    private synchronized void record(AdminAction action) {
        history.addFirst(action);
        if (history.size() > MAX_HISTORY) {
            history.removeLast();
        }
    }
}
