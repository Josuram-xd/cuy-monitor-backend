package com.cuymonitor.backend.domain.health.chain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <b>Builder.</b> Links the handlers in the order of the {@link #then} calls, which the caller writes out, never
 * injected lists or {@code @Order}. The first handler added is the entry point of the chain.
 */
public final class HandlerChainBuilder {

    private final List<EventHandler> handlers = new ArrayList<>();

    public HandlerChainBuilder then(EventHandler handler) {
        handlers.add(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    public EventHandler build() {
        if (handlers.isEmpty()) {
            throw new IllegalStateException("a chain needs at least one handler");
        }
        for (int i = 0; i < handlers.size() - 1; i++) {
            handlers.get(i).setNext(handlers.get(i + 1));
        }
        return handlers.get(0);
    }
}
