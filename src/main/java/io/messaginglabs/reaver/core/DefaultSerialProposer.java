package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.config.Config;
import io.messaginglabs.reaver.config.GroupConfigs;
import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.group.GroupEnv;
import io.messaginglabs.reaver.group.MultiPaxosGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final List<GenericCommit> buf;
    private final ProposalContext ctx;
    private Sequencer sequencer;
    private State state;

    private GroupConfigs configs;
    private InstanceCache cache;

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

    @Override public State state() {
        return null;
    }

    @Override public List<GenericCommit> newBatch() {
        return null;
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

        if (batch != buf) {
            throw new IllegalStateException("buggy, batch is not buffer");
        }

        /*
         * now, try to proposal a new proposal containing the given batch
         */
        this.state = State.PROPOSING;

        if (!newProposal(batch)) {
            fail(batch);
            return ;
        }

        if (!proposeRightNow()) {
            delay();

            /*
             * statistics and add events if necessary
             */

            return ;
        }

        doPropose();
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

        state = State.FREE;
    }

    private void doPropose() {

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
    private final Runnable prepareRunner = this::doPropose;

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
}
