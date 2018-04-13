package io.messaginglabs.reaver.dsl;

import io.messaginglabs.reaver.config.ConfigEventsListener;
import io.messaginglabs.reaver.config.ConfigView;
import io.messaginglabs.reaver.config.Member;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.core.FollowContext;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Future;
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

    enum Role {

        UNKNOWN,

        /*
         * it's able to propose value through this current and this current is
         * also vote for values.
         */
        FORMAL,

        /*
         * this current only learn chosen values from a provider.
         */
        FOLLOWER,
    }

    int id();

    /**
     * Returns the role of this group
     */
    Role role();

    /**
     * Allocates a buffer from group for avoiding memory copy and unnecessary
     * memory allocation.
     */
    ByteBuf newBuffer(int capacity);

    /**
     * Commits a value, the learn handle is used to know the result in asynchronous
     * way(By invoking {@link Commit#addListener(Consumer)}).
     */
    Commit commit(ByteBuffer value);
    Commit commit(ByteBuf value);

    /**
     * Commits a myValue and attach a optional attachment. the myValue and attachment
     * is applied to the state machine registered to this group once the value was chosen.
     */
    CommitResult commit(ByteBuffer value, Object att);
    CommitResult commit(ByteBuf value, Object att);

    /* config interface */

    ConfigView view();

    /**
     * Selects a donor from ths given member list and learn chosen values from it. The follower
     * try to match a new donor from last config view if the old donor is inactive.
     *
     * Calling {@link #join(List)} if this current wants to be an acceptor(proposer).
     */
    FollowContext follow(List<Node> nodes);

    /**
     * Joins the group though the given seed nodes
     */
    void join(List<Node> members);
    Future<Boolean> leave();
    void recommend(Node newLeader);

    void add(ConfigEventsListener listener);
    void remove(ConfigEventsListener listener);

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