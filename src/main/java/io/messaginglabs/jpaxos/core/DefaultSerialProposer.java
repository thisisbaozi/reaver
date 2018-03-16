package io.messaginglabs.jpaxos.core;

import io.messaginglabs.jpaxos.config.Config;
import io.messaginglabs.jpaxos.config.Configs;
import io.messaginglabs.jpaxos.dsl.Commit;
import io.messaginglabs.jpaxos.dsl.CommitStage;
import io.messaginglabs.jpaxos.group.GroupEnv;
import io.messaginglabs.jpaxos.group.MultiPaxosGroup;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSerialProposer extends AbstractVoter implements SerialProposer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSerialProposer.class);

    private final int id;
    private final GroupEnv env;
    private final List<ValueCommit> buf;
    private final ProposalContext ctx;
    private Sequencer sequencer;
    private State state;

    private Configs configs;

    /*
     * hooks
     */
    private Function<ProposalContext, Boolean> beforePropose;

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
        this.buf = new ArrayList<>();
        this.ctx = new ProposalContext();

        this.state = State.FREE;
    }

    @Override public ValueCommit commit() {
        return null;
    }

    @Override public State state() {
        return null;
    }

    @Override public List<ValueCommit> newBatch() {
        return null;
    }

    @Override
    public void commit(List<ValueCommit> batch) {
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

        if (batch != buf) {
            throw new IllegalStateException("buggy, batch is not buffer");
        }

        /*
         * now, try to proposal a new proposal containing the given batch
         */
        this.state = State.PROPOSING;

        ProposalContext ctx = newProposal(batch);
        ctx.stage(CommitStage.READY);

        /*
         * should we slow down?
         */
        if (!proposeRightNow()) {
            delay();
            return ;
        }

        prepare();
    }

    private void prepare() {
        if (state == State.FREE) {
            /*
             * nothing need to prepare
             */
            // if (env.debug) {
                throw new IllegalStateException(
                    String.format("nothing need to prepare for proposer(%s)", toString())
                );
            // }

            return ;
        }

        CommitStage stage = ctx.stage();
        if (stage != CommitStage.PREPARE) {
            throw new IllegalStateException(
                String.format("buggy, commit's stage(%s) is not PREPARE, proposer(%s)", stage.name(), toString())
            );
        }

        /*
         * finds a config based on the sequence number
         */
        Config config = configs.get(ctx.sequence());
        if (config == null) {
            throw new IllegalStateException(
                String.format("buggy, can't find config works for sequence(%d), proposer(%s)", ctx.sequence(), toString())
            );
        }


    }

    private AlgorithmPhase getPhase() {
        return AlgorithmPhase.THREE_PHASE;
    }

    private void broadcast() {
        if (getPhase() == AlgorithmPhase.TWO_PHASE) {
            broadcast2Phase();
        } else {
            broadcast3Phase();
        }
    }

    private void broadcast3Phase() {

    }

    private void broadcast2Phase() {

    }

    /*
     * for reducing memory footprint
     */
    private final Runnable prepareRunner = this::prepare;

    private void delay() {
        if (logger.isDebugEnabled()) {
            logger.info("delay {}ms to prepare proposal({}), proposer({})", delayPeriod, ctx.toString(), toString());
        }

        /*
         * todo: do statistics
         */

        ScheduledExecutorService executor = env.executor;
        try {
            executor.schedule(prepareRunner, delayPeriod, TimeUnit.MILLISECONDS);
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

        boolean result = false;
        try {
            result = beforePropose.apply(ctx);
        } catch (Exception cause) {
            /*
             * a bug?
             */
            logger.error("caught unknown exception while invoking before prepare hook", cause);
        }

        return result;
    }

    private ProposalContext newProposal(List<ValueCommit> batch) {
        /*
         * find a sequence number for a new proposal, must ensure that
         * no proposal associated with the new sequence.
         */
        ctx.reset(sequencer.next(), batch);
        return ctx;
    }

    @Override
    public void observe(State state, Consumer<SerialProposer> consumer) {

    }

    @Override
    public Commit commit(ByteBuffer value) {
        return null;
    }

    @Override
    public ExecutorService executor() {
        return null;
    }

    @Override
    public MultiPaxosGroup group() {
        return null;
    }
}
