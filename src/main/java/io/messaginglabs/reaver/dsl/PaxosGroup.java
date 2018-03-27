package io.messaginglabs.reaver.dsl;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface PaxosGroup {

    enum State {
        NOT_STARTED,
        RUNNING,
        FROZEN,
        SHUTTING_DOWN,
        SHUTDOWN,
        DESTROYED,
    }

    int id();

    /**
     * Registers a state machine with this group, the registered state machine
     * is response for processing chosen values.
     */
    void register(StateMachine machine);

    /**
     * Commits a value, the commit handle is used to know the result in asynchronous
     * way(By invoking {@link Commit#addListener(Consumer)}).
     */
    Commit commit(ByteBuffer value);

    /**
     * Commits a value and attach a optional attachment. the value and attachment
     * is applied to the state machine registered to this group once the value was chosen.
     */
    CommitResult commit(ByteBuffer value, Object att);

    ConfigControl config();
    GroupStatistics statistics();

    /**
     * Returns the current state of this group
     */
    State state();

    /**
     * Closes this group, refuses all commits and config control once a group
     * is closed.
     */
    void close(long timeout) throws Exception;

    /**
     * Tell whether or not this group is closed.
     */
    boolean isClosed();

    /**
     * Destroy this group, all metadata and log files associated with this group
     * will be deleted.
     */
    void destroy() throws Exception;

}