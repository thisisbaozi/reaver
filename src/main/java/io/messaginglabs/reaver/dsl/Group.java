package io.messaginglabs.reaver.dsl;

import io.messaginglabs.reaver.config.ConfigControl;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface Group {

    int id();

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
     * Closes this group, refuses all commits and config control once a group
     * is closed.
     */
    void close();

    /**
     * Tell whether or not this group is closed.
     */
    boolean isClosed();

    /**
     * Destroy this group, all metadata and log files associated with this group
     * will be deleted.
     */
    void destroy();

}