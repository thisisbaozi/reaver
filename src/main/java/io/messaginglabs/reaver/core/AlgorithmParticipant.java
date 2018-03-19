package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.debug.RunningEvent;
import io.messaginglabs.reaver.debug.RunningEvents;
import io.messaginglabs.reaver.group.PaxosGroup;

public abstract class AlgorithmParticipant implements Participant {

    /*
     * the thread is response for processing
     */
    private Thread thread;

    protected void inLoop() {
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


    @Override public PaxosGroup group() {
        return null;
    }

    @Override public boolean isDebug() {
        return false;
    }

    @Override public RunningEvents events() {
        return null;
    }

    @Override public void add(RunningEvent event) {

    }
}
