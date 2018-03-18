package io.messaginglabs.reaver.debug;

import java.util.concurrent.TimeUnit;

public interface RunningEvent {

    /**
     * Returns the time when this event taken place, the time unit is
     * {@link TimeUnit#NANOSECONDS}
     */
    long time();

    /**
     * Returns the thread creating this event.
     */
    Thread thread();

}
