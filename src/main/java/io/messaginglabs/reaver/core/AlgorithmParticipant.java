package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.debug.RunningEvent;
import io.messaginglabs.reaver.debug.RunningEvents;
import io.messaginglabs.reaver.group.PaxosGroup;

public abstract class AlgorithmParticipant implements Participant {

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
