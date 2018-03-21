package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.PrepareReply;
import io.messaginglabs.reaver.com.msg.Propose;
import io.messaginglabs.reaver.com.msg.ProposeReply;
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
import java.util.concurrent.ScheduledFuture;
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
    private final ProposeContext ctx;
    private final Sequencer sequencer;
    private final ByteBuf buffer;
    private final Propose propose;
    private PaxosInstance instance;

    private State state;
    private Function<ProposeContext, Boolean> beforePropose;
    private ScheduledFuture<?> retry;
    private int delay = 3000;

    /*
     * for reducing memory footprint
     */
    private final Runnable proposeRunner = this::propose;
    private final Runnable retryRunner = this::proposeAgain;

    public DefaultSerialProposer(int id, Sequencer sequencer, GroupEnv env) {
        this.id = id;
        this.env = Objects.requireNonNull(env, "env");
        this.sequencer = Objects.requireNonNull(sequencer, "sequencer");

        /*
         * ArrayList is enough, thread-safe is not necessary
         */
        this.cache = new ArrayList<>();
        this.ctx = new ProposeContext();
        this.propose = new Propose();
        this.buffer = env.allocator.buffer(1024 * 4);

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
    public void setProposeBeforeHook(Function<ProposeContext, Boolean> processor) {
        this.beforePropose = processor;
    }

    @Override
    public void setDelay(int time) {
        this.delay = time;
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
         * 0. serializes ctx to a buffer, and wrap the buffer with a message
         * 1. broadcasts the message mentioned above
         * 2. schedules a task for proposing the ctx again once the previous is timeout
         *    or rejected.
         */
        propose(ctx);

        /*
         * try to propose again if the ctx is not accepted after
         * a period.
         */
        retryAfter(delay);
    }

    private void retryAfter(long timeout) {
        retry = env.executor.schedule(retryRunner, timeout, TimeUnit.MILLISECONDS);
    }

    private void proposeAgain() {
        /*
         * possible causes:
         *
         * 0. rejected by a number of acceptors
         * 1. timeout due to network trouble
         * 2. the value in the instance has been chosen, but it's not the value
         *    this proposer proposed
         */
        if (env.debug) {
            logger.info(
                "propose instance({}) again, the previous round(acceptors({}), refused({}), max proposal({}))",
                ctx.instanceId(),
                ctx.acceptCounter().dumpAccepted(),
                ctx.acceptCounter().dumpRefused(),
                ctx.maxPromised().toString()
            );
        }

        proposeIn3Phase(ctx);
    }

    private AlgorithmPhase getPhase() {
        if (env.phase == AlgorithmPhase.THREE_PHASE) {
            return AlgorithmPhase.THREE_PHASE;
        }

        return ctx.phase();
    }

    private void propose(ProposeContext ctx) {
        Objects.requireNonNull(ctx, "ctx");

        if (getPhase() == AlgorithmPhase.TWO_PHASE) {
            proposeIn2Phase(ctx);
        } else {
            proposeIn3Phase(ctx);
        }
    }

    private void validate(ProposeContext ctx) {
        Objects.requireNonNull(ctx, "ctx");

        Config config = ctx.config();
        if (config == null) {
            throw new IllegalStateException(
                String.format("no config in ctx(%s)", ctx.toString())
            );
        }

        Node node = config.node();
        if (node == null) {
            throw new IllegalStateException(
                String.format("buggy, node is null in config(%s)", config.toString())
            );
        }

        long instanceId = ctx.instanceId();
        if (instanceId == -1) {
            throw new IllegalArgumentException(
                String.format("instance id is -1, invalid ctx(%s)", ctx.toString())
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
        propose.setNodeId(config.node().id());
        propose.setGroupId(group().id());
        propose.setInstanceId(proposal.instanceId());
        propose.setValue(buffer);
        propose.setOp(io.messaginglabs.reaver.com.msg.Message.Operation.PREPARE);

        config.broadcast(propose);
    }

    private void proposeLater() {
        if (logger.isDebugEnabled()) {
            logger.info("proposeLater {}ms to prepare ctx({}), proposer({})", delay, ctx.toString(), toString());
        }

        ScheduledExecutorService executor = env.executor;
        try {
            executor.schedule(proposeRunner, delay, TimeUnit.MILLISECONDS);
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
                logger.warn("can't schedule proposing task({}) of proposer({}) due to too many tasks in executor", ctx.toString(), toString());

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
            result = beforePropose.apply(ctx);
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
         * find a sequence number for a new ctx, must ensure that
         * no ctx associated with the new sequence.
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

        ctx.reset(instanceId, batch, config);
        return true;
    }

    @Override
    public void observe(State state, Consumer<SerialProposer> consumer) {

    }

    @Override
    public MultiPaxosGroup group() {
        return null;
    }

    @Override
    public void process(PrepareReply reply) {
        Objects.requireNonNull(reply, "reply");

        if (instance == null) {
            /*
             * Either the proposal this proposer proposed is completed and it
             * is not yet to propose a new one, or it doesn't propose any one.
             */
            if (env.debug) {
                logger.info("ignore reply({}), can't find a instance associated with it", reply.toString());
            }

            return ;
        }

        if (reply.getInstanceId() != instance.id()) {
            if (env.debug) {
                logger.info("ignore prepare reply({}), current proposal({})", reply.toString(), ctx.toString());
            }

            return ;
        }

        Proposal proposed = instance.proposed();

    }

    @Override
    public void process(ProposeReply reply) {
        Objects.requireNonNull(reply, "reply");
    }

    @Override
    public void close() {
        /*
         * release resource
         */
        buffer.release();

        /*
         * if a timer scheduled, cancel it.
         */
        if (retry != null) {
            if (!retry.isCancelled()) {
                retry.cancel(false);
            }
        }
    }
}
