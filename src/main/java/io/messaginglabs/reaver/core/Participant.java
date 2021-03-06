package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.debug.RunningEvent;
import io.messaginglabs.reaver.debug.RunningEvents;

public interface Participant {

    /**
     * Returns true iff this participant run in debug mode, otherwise returns
     * false.
     */
    boolean isDebug();

    /**
     * RunningEvents is used to trace the context of executing
     *
     * note that this only works in debug mode, the best practice is:
     *
     * if (isDebug()) {
     *     RunningEvents events = events();
     *     events.addEvent(event);
     * }
     */
    RunningEvents events();

    /**
     * Adds a non-null event, throws a {@link IllegalStateException} if this participant
     * is not run in debug mode.
     *
     * refers to {@link #events()} for more details about this
     */
    void add(RunningEvent event);

}
