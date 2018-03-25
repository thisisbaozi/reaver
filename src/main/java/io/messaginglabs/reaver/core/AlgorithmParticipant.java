package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.debug.RunningEvent;
import io.messaginglabs.reaver.debug.RunningEvents;
import io.messaginglabs.reaver.group.PaxosGroup;
import java.util.Objects;

public abstract class AlgorithmParticipant implements Participant {

    /*
     * the group this algorithm participant belongs to
     */
    protected final PaxosGroup group;

    /*
     * the thread is response for processing
     */
    private Thread thread;

    public AlgorithmParticipant(PaxosGroup group) {
        this.group = Objects.requireNonNull(group, "group");
    }

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
    public PaxosGroup group() {
        return group;
    }

    @Override
    public boolean isDebug() {
        return group.env().debug;
    }

    @Override
    public RunningEvents events() {
        return null;
    }

    @Override
    public void add(RunningEvent event) {
        if (!isDebug()) {
            throw new IllegalStateException(
                String.format("group(%d) is not run in debug mode, can't add running events", group.id())
            );
        }
    }

}
