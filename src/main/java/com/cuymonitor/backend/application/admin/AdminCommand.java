package com.cuymonitor.backend.application.admin;

/**
 * <b>Command pattern — Command.</b> One change an admin asks for, packed as an object: it carries its receiver
 * and its arguments, so {@link AdminCommandInvoker} can run it and remember it without knowing what it does.
 *
 * <p>Not to be confused with the {@code *Command} records in {@code domain.port.in}: those only carry data
 * (parameter objects); these carry the action itself in {@link #execute()}.
 *
 * @param <R> what the change returns to the admin panel
 */
public interface AdminCommand<R> {

    /** Does the change. Throws, and changes nothing, if a rule refuses it. */
    R execute();

    /** What the change did, for the history ("disable account 3f1c…"). */
    String description();
}
