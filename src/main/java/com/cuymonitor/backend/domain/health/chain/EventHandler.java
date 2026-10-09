package com.cuymonitor.backend.domain.health.chain;

import java.util.Objects;

public abstract class EventHandler {

    private EventHandler next;

    public final EventHandler setNext(EventHandler next) {
        this.next = Objects.requireNonNull(next, "next");
        return next;
    }

    public final EventHandlerContext handle(EventHandlerContext context) {
        Objects.requireNonNull(context, "context");
        if (!context.accepted()) {
            return context;
        }

        handleCurrent(context);
        if (context.accepted() && next != null) {
            next.handle(context);
        }
        return context;
    }

    protected abstract void handleCurrent(EventHandlerContext context);
}
