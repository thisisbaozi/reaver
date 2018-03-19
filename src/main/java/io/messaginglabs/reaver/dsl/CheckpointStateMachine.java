package io.messaginglabs.reaver.dsl;

public interface CheckpointStateMachine extends StateMachine {

    /**
     * Flush any changed applied to this state machine, once flushing operation
     * is completed, this state machine should save the checkpoint(instanceId).
     */
    void flush(long instanceId);

    /**
     * Returns true if the Paxos applier should invoke {@link #flush(long)},
     * otherwise, returns false.
     */
    boolean flushRightNow();

    /**
     * Returns the checkpoint(a instance id) this state machine maintained, returns -1
     * if nothing maintained.
     */
    long getCheckpoint();

}
