package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.exception.TooManyAttemptsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts failures per key (a username, a challenge, an IP) and blocks the key for the rest of its window once it
 * reaches the limit. In memory.
 * shortcut: the counters live in this instance only, so with several instances the real limit is multiplied by the
 * number of instances; move them to a shared store (Redis, the database) before scaling out.
 */
public class FailedAttemptLimiter {

    private static final String IP_PREFIX = "ip:";
    private static final int PURGE_ABOVE = 10_000;

    private record Window(Instant start, int failures) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxPerKey;
    private final int maxPerIp;
    private final Duration window;
    private final Clock clock;

    public FailedAttemptLimiter(int maxPerKey, int maxPerIp, Duration window, Clock clock) {
        this.maxPerKey = maxPerKey;
        this.maxPerIp = maxPerIp;
        this.window = window;
        this.clock = clock;
    }

    /** Throws if the key (a username, a challenge) or the IP already failed too many times in its window. */
    public void check(String key, String ip) {
        check(key, maxPerKey);
        check(IP_PREFIX + ip, maxPerIp);
    }

    public void recordFailure(String key, String ip) {
        recordFailure(key);
        recordFailure(IP_PREFIX + ip);
    }

    private void check(String key, int max) {
        Instant now = clock.instant();
        Window current = windows.get(key);
        if (current != null && current.failures() >= max && now.isBefore(current.start().plus(window))) {
            throw new TooManyAttemptsException(Duration.between(now, current.start().plus(window)));
        }
    }

    private void recordFailure(String key) {
        Instant now = clock.instant();
        if (windows.size() > PURGE_ABOVE) {
            windows.values().removeIf(w -> !now.isBefore(w.start().plus(window)));
        }
        windows.merge(key, new Window(now, 1),
                (old, fresh) -> now.isBefore(old.start().plus(window)) ? new Window(old.start(), old.failures() + 1) : fresh);
    }

    public void reset(String key) {
        windows.remove(key);
    }
}
