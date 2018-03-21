package io.messaginglabs.reaver.dsl;

import java.util.Iterator;

public interface StateMachine {

    /* process interfaces */

    /**
     * Applies a number of chosen instances
     */
    void apply(Iterator<ChosenValues> instances);

    /**
     * Failed to propose the commit
     */
    void process(CommitResult cause, Value value);

    /**
     * processing critical errors, it's notified while a error taken
     * place in Paxos. Stop to propose any values once this method invoked.
     */
    void onPanic(PaxosError error);

    /* snapshot interfaces */

    /**
     * Install the given snapshot to this state machine, this state machine is able
     * to process chosen instances in order once the snapshot is installed.
     */
    void install(Snapshot snapshot);

    /**
     * Creates a snapshot from this state machine.
     */
    void create(SnapshotBuilder builder);

    /**
     * Invoked when the Paxos group this state machine registered to is closed
     */
    void onClosed();

}
