package io.messaginglabs.jpaxos.group;

import io.messaginglabs.jpaxos.com.Message;
import io.messaginglabs.jpaxos.config.ConfigControl;
import io.messaginglabs.jpaxos.config.Nodes;
import io.messaginglabs.jpaxos.core.ParallelProposer;
import io.messaginglabs.jpaxos.core.Proposer;
import io.messaginglabs.jpaxos.dsl.ClosedGroupException;
import io.messaginglabs.jpaxos.dsl.Commit;
import io.messaginglabs.jpaxos.dsl.FrozenGroupException;
import io.messaginglabs.jpaxos.dsl.Group;
import io.messaginglabs.jpaxos.dsl.GroupState;
import io.messaginglabs.jpaxos.dsl.GroupStatistics;
import io.messaginglabs.jpaxos.utils.Parameters;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiPaxosGroup implements PaxosGroup {

    private static final Logger logger = LoggerFactory.getLogger(MultiPaxosGroup.class);

    private final GroupEnv env;
    private final int id;

    private final GroupState state;
    private final GroupOptions options;

    /*
     * algorithm participants
     */
    private Proposer proposer;

    public MultiPaxosGroup(int id, GroupEnv env, GroupOptions options) {
        this.env = Objects.requireNonNull(env, "env");
        this.options = Objects.requireNonNull(options, "options");
        this.id = id;
        this.state = GroupState.NOT_STARTED;

        validate();
    }

    private void validate() {

    }

    public int id() {
        return id;
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

    private void initParticipants() {
        // proposer
        proposer = new ParallelProposer(options.valueCacheCapacity, options.batch, options.pipeline);

        // acceptor

        // learner
    }

    private void startTasks() {

    }

    public Commit commit(ByteBuffer value) {
        if (state == GroupState.NOT_STARTED) {
            throw new IllegalArgumentException(String.format("init group(%s) before committing values to it", id));
        }

        if (state == GroupState.FROZEN) {
            throw new FrozenGroupException(String.format("group(%d) is frozen", id));
        }

        if (state == GroupState.SHUTTING_DOWN || state == GroupState.SHUTDOWN) {
            throw new ClosedGroupException(id + " is shutdown");
        }

        return proposer.commit(value);
    }

    @Override
    public ConfigControl config() {
        return null;
    }

    @Override
    public GroupStatistics statistics() {
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

    public boolean slowDown() {
        return false;
    }

    @Override
    public void process(Message msg) {

    }

}
