package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.Message;
import io.messaginglabs.reaver.config.ConfigControl;
import io.messaginglabs.reaver.core.ParallelProposer;
import io.messaginglabs.reaver.core.Proposer;
import io.messaginglabs.reaver.dsl.ClosedGroupException;
import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.FrozenGroupException;
import io.messaginglabs.reaver.dsl.GroupState;
import io.messaginglabs.reaver.dsl.GroupStatistics;
import java.nio.ByteBuffer;
import java.util.Objects;
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
