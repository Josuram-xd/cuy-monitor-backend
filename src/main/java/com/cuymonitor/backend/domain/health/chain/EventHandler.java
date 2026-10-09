package com.cuymonitor.backend.domain.health.chain;

public abstract class EventHandler {

    private EventHandler next;

    // returns the handler it receives so the chain can be linked in one line
    public EventHandler setNext(EventHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(EventContext context) {
        process(context);
        if (!context.isDropped() && next != null) {
            next.handle(context);
        }
    }

    protected abstract void process(EventContext context);
}
