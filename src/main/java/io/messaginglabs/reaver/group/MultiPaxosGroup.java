package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.config.Config;
import io.messaginglabs.reaver.config.ConfigEventsListener;
import io.messaginglabs.reaver.config.ConfigView;
import io.messaginglabs.reaver.config.GroupConfigControl;
import io.messaginglabs.reaver.config.MetadataStorage;
import io.messaginglabs.reaver.config.PaxosGroupConfigs;
import io.messaginglabs.reaver.config.SimpleConfigStorage;
import io.messaginglabs.reaver.core.Acceptor;
import io.messaginglabs.reaver.core.Applier;
import io.messaginglabs.reaver.core.FollowContext;
import io.messaginglabs.reaver.core.InstanceCache;
import io.messaginglabs.reaver.core.Learner;
import io.messaginglabs.reaver.dsl.ConfigControl;
import io.messaginglabs.reaver.config.GroupConfigs;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.core.Proposer;
import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.dsl.GroupStatistics;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.netty.util.ReferenceCounted;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiPaxosGroup implements InternalPaxosGroup {

    private static final Logger logger = LoggerFactory.getLogger(MultiPaxosGroup.class);

    // global unique identifier
    private final int id;
    private PaxosGroup.State state;
    private PaxosGroup.Role role;

    private final GroupEnv env;
    private final GroupOptions options;

    private String msg;

    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;
    private Applier applier;
    private StateMachine sm;

    // config
    private final GroupConfigs configs;

    private Runnable closeListener;

    public MultiPaxosGroup(int id, StateMachine sm,  GroupEnv env, GroupOptions options) {
        this.env = Objects.requireNonNull(env, "env");
        this.options = Objects.requireNonNull(options, "options");
        this.sm = Objects.requireNonNull(sm, "stateMachine");

        this.id = id;
        this.state = State.NOT_STARTED;
        this.role = Role.UNKNOWN;

        MetadataStorage storage = null;
        if (options.reserveConfig) {
            storage = env.metadataStorage;
        } else {
            /*
             * Disabling storing config does not affect the correctness of
             * Paxos
             */
            logger.info("group({}) do not store config", id);
        }

        configs = new PaxosGroupConfigs(this, storage);
    }

    @Override
    public final State state() {
        return state;
    }

    public final int id() {
        return id;
    }

    @Override
    public final Role role() {
        return role;
    }

    @Override
    public Future<ConfigView> join(List<Node> members) {
        synchronized (configs) {
            Config config = configs.newest();
        }
        return null;
    }

    @Override
    public long maxInstanceId(long instanceId) {
        return 0;
    }

    @Override
    public long maxInstanceId() {
        return 0;
    }

    @Override
    public GroupOptions options() {
        return null;
    }

    @Override
    public boolean isSlowDown(long instanceId) {
        /*
         * There's only one factor we need to consider currently:
         *
         * 0. too many finished instances in cache(Applier is too slow)
         */
        return true;
    }

    @Override
    public void freeze(String msg) {
        this.state = State.FROZEN;
        this.msg = msg;
    }

    @Override
    public int pendingCompletedInstances() {
        return 0;
    }

    @Override
    public InstanceCache cache() {
        return null;
    }

    @Override
    public Server server() {
        return null;
    }

    @Override
    public void start() {

    }

    private void startTasks() {
        // heartbeat task

        // detect task

        //
    }

    @Override
    public void boot() {

    }

    @Override
    public Node local() {
        return null;
    }

    private void checkState(ByteBuffer value) {
        if (state != State.RUNNING) {
            throw new IllegalStateException(
                String.format("group(%d) is not running(%s)", id, state.name())
            );
        }

        Objects.requireNonNull(value, "value");
        if (value.remaining() == 0) {
            throw new IllegalArgumentException("can't commit empty value");
        }

        /*
         * checks whether this node is able to commit or not
         */
        if (proposer == null) {
            throw new IllegalStateException(
                String.format("node(%s) in group(%d) is just a learner", local().toString(), id)
            );
        }
    }

    public Commit commit(ByteBuffer value) {
        checkState(value);
        return proposer.commit(value);
    }

    @Override
    public CommitResult commit(ByteBuffer value, Object att) {
        checkState(value);
        return proposer.commit(value, att);
    }

    @Override
    public GroupStatistics statistics() {
        return null;
    }

    public GroupEnv env() {
        return env;
    }

    @Override
    public GroupConfigs configs() {
        return null;
    }

    @Override
    public void process(Message msg) {
        Objects.requireNonNull(msg, "msg");

        int dstId = msg.getGroupId();
        if (dstId != id) {
            throw new IllegalStateException(
                String.format("buggy, group(%d) can't process messages belong to group(%d)", id, dstId)
            );
        }

        if (state != State.RUNNING) {
            if (logger.isDebugEnabled()) {
                logger.debug("group({}) ignores message({}), it's not running({})", id, msg.toString(), state.name());
            }

            return ;
        }

        dispatch(msg);
    }

    private void dispatch(Message msg) {

    }

    @Override public ConfigView view() {
        return null;
    }

    @Override public FollowContext follow(List<Node> nodes) {
        return null;
    }


    @Override public Future<Boolean> leave() {
        return null;
    }

    @Override public void add(ConfigEventsListener listener) {

    }

    @Override public void remove(ConfigEventsListener listener) {

    }

    @Override
    public void close(long timeout) throws Exception {
        synchronized (this) {
            if (isClosed()) {
                return ;
            }

            state = State.SHUTTING_DOWN;
            doClose(timeout);
            state = State.SHUTDOWN;
        }
    }

    private void doClose(long timeout) throws Exception {
        /*
         * 5 things:
         *
         * 0. stop participants
         * 1. shutdown tasks
         * 2. notify state machine
         * 3. releases resources
         * 4. invoke listener if any
         */
        stopParticipants(timeout);
        shutdownTasks();

        try {
            sm.onGroupClosed();
        } catch (Exception cause) {
            logger.warn("caught unknown exception while notifying state machine", cause);
        }

        env.connector.release();
        configs.serversConnected().forEach(ReferenceCounted::release);
        env.storage.release();
        env.transporter.release();

        if (closeListener != null) {
            try {
                closeListener.run();
            } catch (Exception cause) {
                if (logger.isWarnEnabled()) {
                    logger.warn("caught unknown exception while executing close listener", cause);
                }
            }
        }
    }

    private void stopParticipants(long timeout) throws Exception {
        // follower has not proposer and acceptor
        if (proposer != null) {
            if (!proposer.close(timeout) && timeout > 0) {
                logger.warn("can't close proposer({}) in specified time({})", id, timeout);
            }
        }
        if (acceptor != null) {
            close(acceptor, String.format("acceptor of group(%d)", id));
        }

        close(learner, String.format("leaner of group(%d)", id));
        close(applier, String.format("applier of group(%d)", id));
    }

    private void close(AutoCloseable src, String id) throws Exception {
        logger.info("Starts to close {}", id);
        long begin = System.currentTimeMillis();
        src.close();
        logger.info("{} has been closed, execution time({})", id, System.currentTimeMillis() - begin);
    }

    private void shutdownTasks() {

    }

    @Override
    public void addCloseListener(Runnable runner) {
        this.closeListener = runner;
    }

    @Override
    public StateMachine getStateMachine() {
        return null;
    }

    @Override
    public void destroy() throws Exception {
        synchronized (this) {
            if (isClosed()) {
                close(-1);
            }

            /*
             * 2 things:
             *
             * 0. delete metadata
             * 1. delete log data if we can
             */

            state = State.DESTROYED;
        }
    }

    @Override
    public boolean isClosed() {
        return state == State.SHUTTING_DOWN || state == State.SHUTDOWN;
    }

    @Override
    public String toString() {
        return null;
    }

}
