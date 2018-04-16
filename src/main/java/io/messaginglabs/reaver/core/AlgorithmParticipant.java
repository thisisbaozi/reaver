package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.debug.RunningEvent;
import io.messaginglabs.reaver.debug.RunningEvents;

public abstract class AlgorithmParticipant implements Participant {

    /*
     * the thread is response for processing
     */
    private Thread thread;
    private boolean debug = false;

    void inLoop() {
        if (isDebug()) {
            Thread current = Thread.currentThread();
            if (thread == null) {
                thread = current;
                return;
            }

            if (thread != current) {
                throw new IllegalStateException(
                    String.format("expect thread(%s), but %s", thread.toString(), current.toString())
                );
            }
        }
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public RunningEvents events() {
        return null;
    }

    @Override
    public void add(RunningEvent event) {

    }

}
