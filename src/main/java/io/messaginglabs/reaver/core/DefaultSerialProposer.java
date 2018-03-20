package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.Propose;
import io.messaginglabs.reaver.config.Config;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.group.GroupEnv;
import io.messaginglabs.reaver.group.MultiPaxosGroup;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSerialProposer extends AlgorithmVoter implements SerialProposer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSerialProposer.class);

    private final int id;
    private final GroupEnv env;

    @SuppressWarnings("all")
    private final List<GenericCommit> cache;

    /*
     * the proposal this proposer is proposing
     */
    private final ProposeContext proposal;
    private AlgorithmPhase phase = AlgorithmPhase.THREE_PHASE;
    private Sequencer sequencer;
    private State state;

    private ByteBuf buffer;

    private Propose propose = new Propose();
    private long retryTimeout;

    /*
     * hooks
     */
    private Function<ProposeContext, Boolean> beforePropose;

    /*
     * prepare again after delayed period
     */
    private int delayPeriod = 3;

    public DefaultSerialProposer(int id, GroupEnv env) {
        this.id = id;
        this.env = Objects.requireNonNull(env, "env");

        /*
         * ArrayList is enough, thread-safe is not necessary
         */
        this.cache = new ArrayList<>();
        this.proposal = new ProposeContext();

        this.state = State.FREE;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public List<GenericCommit> newBatch() {
        return cache;
    }

    @Override
    public void commit(List<GenericCommit> batch) {
        Objects.requireNonNull(batch, "batch");

        if (batch.isEmpty()) {
            throw new IllegalArgumentException("no values");
        }

        State state = state();
        if (state != State.FREE) {
            throw new IllegalStateException(
                String.format("buggy, serial proposer(%d) is not free(%s)", id, state.name())
            );
        }

        if (batch != cache) {
            throw new IllegalStateException("buggy, batch is not the buffer of this proposer");
        }

        this.state = State.PROPOSING;

        if (!newProposal(batch)) {
            try {
                fail(batch);
            } finally {
                this.state = State.FREE;
            }

            return ;
        }

        if (!proposeRightNow()) {
            proposeLater();

            /*
             * statistics and add events if necessary
             */

            return ;
        }

        propose();
    }

    private void fail(List<GenericCommit> commits) {
        Objects.requireNonNull(commits, "commits");

        /*
         * statistics and trace
         */

        for (GenericCommit commit : commits) {
            if (commit.isCancelled() || commit.isDone()) {
                throw new IllegalStateException(
                    String.format("can't mark a commit(%s) that has been done", commit.toString())
                );
            }

            commit.setFailure(CommitResult.NO_CONFIG);
        }
    }

    private void propose() {
        /*
         * 0. serializes proposal to a buffer, and wrap the buffer with a message
         * 1. broadcasts the message mentioned above
         * 2. schedules a task for proposing the proposal again once the previous is timeout
         *    or rejected.
         */
        propose(proposal);

        /*
         * try to propose again if the proposal is not accepted after
         * a period.
         */
        retryAfter(retryTimeout);
    }

    private void retryAfter(long tiumeout) {

    }

    private void proposeAgain() {

    }

    private AlgorithmPhase getPhase() {
        if (phase == AlgorithmPhase.THREE_PHASE) {
            return AlgorithmPhase.THREE_PHASE;
        }

        return proposal.phase();
    }

    private void propose(ProposeContext proposal) {
        Objects.requireNonNull(proposal, "proposal");

        if (getPhase() == AlgorithmPhase.TWO_PHASE) {
            proposeIn2Phase(proposal);
        } else {
            proposeIn3Phase(proposal);
        }
    }

    private void validate(ProposeContext proposal) {
        Objects.requireNonNull(proposal, "proposal");

        Config config = proposal.config();
        if (config == null) {
            throw new IllegalStateException(
                String.format("no config in proposal(%s)", proposal.toString())
            );
        }

        Node node = config.node();
        if (node == null) {
            throw new IllegalStateException(
                String.format("buggy, node is null in config(%s)", config.toString())
            );
        }

        long instanceId = proposal.instanceId();
        if (instanceId == -1) {
            throw new IllegalArgumentException(
                String.format("instance id is -1, invalid proposal(%s)", proposal.toString())
            );
        }
    }

    private void proposeIn2Phase(ProposeContext proposal) {
        validate(proposal);

        Config config = proposal.config();

        propose.setSequence(0);
        propose.setNodeId(config.node().id());
        propose.setGroupId(group().id());
        propose.setInstanceId(proposal.instanceId());
        propose.setValue(buffer);
        propose.setOp(io.messaginglabs.reaver.com.msg.Message.Operation.PROPOSE);

        config.broadcast(propose);

        // add event
    }

    private void proposeIn3Phase(ProposeContext proposal) {
        validate(proposal);

        Config config = proposal.config();

        /*
         * increase sequence if it's necessary
         */
        int sequence = propose.getSequence();
        int maxSequence = proposal.maxPromised().getSequence();
        if (maxSequence > sequence) {
            sequence = maxSequence;
        }

        propose.setSequence(sequence);

        /*
         * add event
         */

        propose.setNodeId(config.node().id());
        propose.setGroupId(group().id());
        propose.setInstanceId(proposal.instanceId());
        propose.setValue(buffer);
        propose.setOp(io.messaginglabs.reaver.com.msg.Message.Operation.PREPARE);

        config.broadcast(propose);

        // add event

        // statistics
    }

    /*
     * for reducing memory footprint
     */
    private final Runnable proposeRunner = this::propose;

    private void proposeLater() {
        if (logger.isDebugEnabled()) {
            logger.info("proposeLater {}ms to prepare proposal({}), proposer({})", delayPeriod, proposal.toString(), toString());
        }

        /*
         * todo: do statistics
         */

        ScheduledExecutorService executor = env.executor;
        try {
            executor.schedule(proposeRunner, delayPeriod, TimeUnit.MILLISECONDS);
        } catch (Exception cause) {
            if (executor.isShutdown()) {
                /*
                 * it's likely that the group has been closed.
                 */
                logger.warn("");
            } else {
                /*
                 * too many tasks?
                 */
                logger.warn("can't schedule proposing task({}) of proposer({}) due to too many tasks in executor", proposal.toString(), toString());

                /*
                 * mark for proposing later
                 */
                state = State.READY_TO_PREPARE;
            }
        }
    }

    private boolean proposeRightNow() {
        if (beforePropose == null) {
            return true;
        }

        /*
         * slow down if:
         *
         * 0. too many chosen values in cache(Applier is too slow)
         */
        boolean result = false;
        try {
            result = beforePropose.apply(proposal);
        } catch (Exception cause) {
            /*
             * a bug?
             */
            logger.error("caught unknown exception while invoking before prepare hook", cause);

            if (isDebug()) {
                System.exit(-1);
            }
        }

        return result;
    }

    private boolean newProposal(List<GenericCommit> batch) {
        /*
         * find a sequence number for a new proposal, must ensure that
         * no proposal associated with the new sequence.
         */
        long instanceId = sequencer.next();

        /*
         * find a config for this batch
         */
        Config config = find(instanceId);
        if (config == null) {
            /*
             * no config, refuse this batch
             */
            return false;
        }

        proposal.reset(instanceId, batch, config);
        return true;
    }

    @Override
    public void observe(State state, Consumer<SerialProposer> consumer) {

    }

    @Override
    public MultiPaxosGroup group() {
        return null;
    }
}
