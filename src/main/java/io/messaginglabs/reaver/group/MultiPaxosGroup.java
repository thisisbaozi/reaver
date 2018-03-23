package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.core.InstanceCache;
import io.messaginglabs.reaver.dsl.ConfigControl;
import io.messaginglabs.reaver.config.GroupConfigs;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.core.Acceptor;
import io.messaginglabs.reaver.core.Learner;
import io.messaginglabs.reaver.core.ParallelProposer;
import io.messaginglabs.reaver.core.Proposer;
import io.messaginglabs.reaver.dsl.ClosedGroupException;
import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.dsl.FrozenGroupException;
import io.messaginglabs.reaver.dsl.Group;
import io.messaginglabs.reaver.dsl.GroupStatistics;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.netty.util.concurrent.ScheduledFuture;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiPaxosGroup implements PaxosGroup {

    private static final Logger logger = LoggerFactory.getLogger(MultiPaxosGroup.class);

    private final GroupEnv env;
    private final int id;
    private final GroupOptions options;

    /*
     * indicate the current state of this group
     */
    private Group.State state;

    // core
    private GroupConfigs configs;
    private Proposer proposer;
    private Acceptor acceptor;
    private Learner localLearner;

    // tasks

    public MultiPaxosGroup(int id, GroupEnv env, GroupOptions options) {
        this.env = Objects.requireNonNull(env, "env");
        this.options = Objects.requireNonNull(options, "options");

        this.id = id;
        this.state = Group.State.NOT_STARTED;

        validate();
    }

    private void validate() {
        if (env.phase == null) {
            throw new IllegalArgumentException("unknown x phase");
        }

        if (env.applier == null) {
            env.applier = env.executor;
        }

        if (env.executor == null) {
            throw new IllegalArgumentException("no executor");
        }
        if (env.executor.isShutdown()) {
            throw new IllegalArgumentException("closed executor");
        }
        if (env.allocator == null) {
            throw new IllegalArgumentException("no memory allocator");
        }
        if (env.codec == null) {
            throw new IllegalArgumentException("no codec");
        }

        if (env.connector == null) {
            throw new IllegalArgumentException("no server connector");
        }
        if (env.storage == null) {
            throw new IllegalArgumentException("no storage");
        }
        if (env.transporter == null) {
            throw new IllegalArgumentException("no transporter");
        }
    }

    public final int id() {
        return id;
    }

    @Override public long maxInstanceId(long instanceId) {
        return 0;
    }

    @Override public long maxInstanceId() {
        return 0;
    }

    @Override public GroupOptions options() {
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

    @Override public void freeze(String msg) {

    }

    @Override public int pendingCompletedInstances() {
        return 0;
    }

    @Override public InstanceCache cache() {
        return null;
    }

    @Override
    public void register(StateMachine machine) {

    }

    @Override
    public void init() {
        /*
         * init components and algorithm participants
         */

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

    private void initParticipants() {
        // proposer
        proposer = new ParallelProposer(options.valueCacheCapacity, options.batch, options.pipeline);

        // acceptor

        // learner
    }



    private void checkState(ByteBuffer value) {
        if (state == Group.State.NOT_STARTED) {
            throw new IllegalArgumentException(String.format("init group(%s) before committing values to it", id));
        }

        if (state == Group.State.SHUTTING_DOWN || state == Group.State.SHUTDOWN) {
            throw new ClosedGroupException(id + " is shutdown");
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
    public ConfigControl config() {
        return null;
    }

    @Override
    public GroupStatistics statistics() {
        return null;
    }

    @Override public State state() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    public GroupEnv env() {
        return env;
    }

    @Override public GroupConfigs configs() {
        return null;
    }

    public boolean slowDown() {
        return false;
    }

    @Override
    public void process(Message msg) {
        // check whether this group should process the given message or not

        // dispatch
    }

}
