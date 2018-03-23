package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.PrepareReply;
import io.messaginglabs.reaver.com.msg.ProposeReply;
import io.messaginglabs.reaver.config.Config;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.dsl.Group;
import io.messaginglabs.reaver.group.GroupEnv;
import io.messaginglabs.reaver.group.PaxosGroup;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSerialProposer extends AlgorithmVoter implements SerialProposer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSerialProposer.class);

    private final int id;

    private final PaxosGroup group;
    private final ProposeContext ctx;

    /*
     * the task is response for checking whether or not a proposal is
     * expired
     */
    private final ScheduledFuture<?> task;

    public DefaultSerialProposer(int id, PaxosGroup group) {
        this.id = id;
        this.group = Objects.requireNonNull(group, "group");

        GroupEnv env = group.env();
        if (env == null) {
            throw new IllegalArgumentException("no group env");
        }

        ByteBuf buf = env.allocator.buffer(1024 * 4);
        if (buf == null) {
            throw new IllegalStateException("bad alloc");
        }

        this.ctx = new ProposeContext();
        this.ctx.set(buf);

        int interval = group.options().retryInterval;
        task = env.executor.scheduleWithFixedDelay(this::propose, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public List<GenericCommit> valueCache() {
        return ctx.valueCache();
    }

    @Override
    public boolean isBusy() {
        return ctx.instanceId() != -1;
    }

    private void fail(List<GenericCommit> commits, CommitResult result) {
        Objects.requireNonNull(result, "result");

        for (GenericCommit commit : commits) {
            if (commit.isDone() || commit.isCancelled()) {
                // a bug?
                throw new IllegalStateException(
                    "commit should be pending, but it's done or cancelled"
                );
            }

            commit.setFailure(result);
        }
    }

    @Override
    public void commit(List<GenericCommit> commits) {
        if (isBusy()) {
            throw new IllegalStateException(
                String.format("buggy, proposer(%d) in group(%d) is processing another proposal", id, group.id())
            );
        }

        if (isClosed()) {
            fail(commits, CommitResult.CLOSED_GROUP);
            return ;
        }

        ctx.setCommits(commits);

        if (isDebug()) {
            logger.trace("set a new commit({}) to proposer({}) of group({})", commits.size(), id, group.id());
        }

        /*
         * Now, this proposer will keep to propose a value composed of
         * the given commits until:
         *
         * 0. the value is chosen.
         * 1. the group this proposer belongs to is closed(fail commits)
         * 2. there's no a config
         */
        propose();
    }

    private void propose() {
        if (group.state() != Group.State.RUNNING) {
            /*
             * fail commits if any
             */
            fail(CommitResult.CLOSED_GROUP);
            return ;
        }

        if (ctx.stage().isReady()) {
            if (ctx.valueCache().isEmpty()) {
                /*
                 * nothing needs to propose
                 */
                if (isDebug()) {
                    logger.trace("nothing need to propose for proposer({}) of group({})", id, group.id());
                }

                return ;
            }

            if (!readyToRun()) {
                return ;
            }

            if (Defines.isValidInstance(ctx.instanceId())) {
                throw new IllegalStateException("buggy, instance is still void");
            }

            propose(ctx);
            return ;
        }

        if (ctx.instance().isDone()) {
            onInstanceDone();
            return ;
        }

        // try again if previous proposing is expired
        long duration = System.currentTimeMillis() - ctx.begin();
        if (duration >= group.options().retryInterval) {
            /*
             * possible causes:
             *
             * 0. rejected by a number of acceptors
             * 1. network trouble
             * 2. the value in the instance has been chosen, but it's not the value
             *    this proposer proposed
             */
            if (isDebug()) {
                logger.info(
                    "propose instance({}) is expired, proposer({}/{}), ratio({}/{}), answered({})",
                    ctx.instanceId(),
                    id,
                    group.id(),
                    "",
                    ctx.config().acceptors(),
                    ctx.acceptCounter().dumpPromised()
                );
            }

            if (ctx.isRefused()) {
                // refused by some one, propose the value based on 3 phases
                proposeIn3Phase(ctx);
            } else {
                // timeout?
                proposeIn2Phase(ctx);
            }
        }
    }

    private boolean readyToRun() {
        /*
         * this proposer is able to go ahead unless:
         *
         * 0. find a free instance
         * 1. it's not necessary to slow down
         */
        long instanceId = acquire();

        if (isDebug()) {
            logger.trace("a free instance({}) for proposer({}) of group({})", instanceId, id, group.id());
        }

        if (group.isSlowDown(instanceId)) {
            int times = ctx.delay();

            if (isDebug()) {
                logger.debug("delay({} times) again for instance({}), delay duration({}ms)", times, ctx.instanceId(), ctx.delayed());
            }

            return false;
        }

        // find a config based on acquired instance id, if no config
        // is available, fail commits
        Config config = find(instanceId);
        if (config == null) {
            if (isDebug()) {
                logger.info("no config is usable for instance({}), fail commits", instanceId);
            }

            fail(CommitResult.NO_CONFIG);

            return false;
        }

        // init Paxos instance
        PaxosInstance instance = group.cache().createIfAbsent(instanceId);
        int holder = instance.hold(id);
        if (holder != id) {
            /*
             * it's a bug, once happened, do:
             *
             * 0. fail commits
             * 1. free group this proposer belongs to
             */
            fail(CommitResult.UNKNOWN_ERROR);

            String msg = String.format(
                "proposer(%d) of group(%d) can't hold the instance(%d), it's hold by another one(%d)",
                id,
                group.id(),
                instanceId,
                holder
            );
            group.freeze(msg);

            return false;
        }

        group.env().sequencer.set(instanceId);
        ctx.reset(instanceId, config, group.local().id());

        return true;
    }

    private void fail(CommitResult result) {
        if (ctx.valueCache().isEmpty()) {
            return ;
        }

        try {
            fail(ctx.valueCache(), result);
        } finally {
            /*
             * in case...
             */
            ctx.clear();
        }
    }

    private void onInstanceDone() {
        Proposal chosen = ctx.instance().chosen();
        if (chosen == null) {
            throw new IllegalStateException(
                String.format("buggy, instance(%s) is still pending", ctx.instance().touch())
            );
        }

        /*
         * ths Paxos instance is done, but the value in this instance may
         * not be the one this proposer proposed
         */
        if (isDebug()) {
            logger.info(ctx.dumpChosenInstance());
        }

        if (chosen.getNodeId() == ctx.nodeId()) {
            // it's enough, group id + node id must be unique
            ctx.clear();

            if (isDebug()) {
                logger.info("the value of proposer({}) of group({}) is chosen, it's ready to propose next one", id, group.id());
            }

            return ;
        }

        /*
         * it's not the value we proposed, try again in a new instance
         */
        propose();
    }



    private AlgorithmPhase getPhase() {
        if (group.env().phase == AlgorithmPhase.THREE_PHASE) {
            return AlgorithmPhase.THREE_PHASE;
        }

        return ctx.phase();
    }

    private void propose(ProposeContext ctx) {
        if (getPhase() == AlgorithmPhase.TWO_PHASE) {
            proposeIn2Phase(ctx);
        } else {
            proposeIn3Phase(ctx);
        }
    }

    private void validate(ProposeContext ctx) {
        Objects.requireNonNull(ctx, "ctx");

        if (ctx.config() == null) {
            throw new IllegalStateException(
                String.format("no config in ctx(%s)", ctx.toString())
            );
        }

        if (ctx.config().node() == null) {
            throw new IllegalStateException(
                String.format("buggy, node is null in config(%s)", ctx.config().toString())
            );
        }

        if (Defines.isValidInstance(ctx.instanceId())) {
            throw new IllegalArgumentException(
                String.format("instance id is -1, invalid ctx(%s)", ctx.toString())
            );
        }
    }

    private void proposeIn2Phase(ProposeContext ctx) {
        validate(ctx);
        ctx.begin(PaxosStage.ACCEPT);

        // Config config = ctx.config();

        // propose.setSequence(0);
        // propose.setNodeId(config.node().id());
        // propose.setGroupId(group().id());
        // propose.setInstanceId(proposal.instanceId());
        // propose.setValue(buffer);
        // propose.setOp(io.messaginglabs.reaver.com.msg.Message.Operation.PROPOSE);

        // config.broadcast(propose);

        // add event
    }

    private void proposeIn3Phase(ProposeContext proposal) {
        validate(proposal);

        Config config = proposal.config();

        /*
         * increase sequence if it's necessary
         */
        /*
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
        */
    }

    private long acquire() {
        /*
         * find a sequence number for a new ctx, must ensure that
         * no ctx associated with the new sequence.
         */
        InstanceCache cache = group.cache();
        if (cache == null) {
            throw new IllegalStateException(String.format("no cache in group(%d)", group.id()));
        }

        int span = 0;
        long instanceId = group.env().sequencer.get();

        for (;;) {
            span++;

            PaxosInstance instance = cache.get(instanceId);
            if (instance == null) {
                /*
                 * no one holds this
                 */
                break;
            }

            /*
             * another proposer in config proposed a proposal in this
             * instance id
             */
            instanceId++;
        }

        if (span > 1 && isDebug()) {
            logger.info("span {} instances for allocating a free instance({}) in proposer({}) of group({})", span, instanceId, id, group.id());
        }

        return instanceId;
    }

    @Override
    public void observe(State state, Consumer<SerialProposer> consumer) {

    }

    @Override
    public void process(PrepareReply reply) {
        Objects.requireNonNull(reply, "reply");

        if (ctx.instance() == null) {
            /*
             * Either the proposal this proposer proposed is completed and it
             * is not yet to propose a new one, or it doesn't propose any one.
             */
            if (isDebug()) {
                logger.info("ignore reply({}), can't find a instance associated with it", reply.toString());
            }

            return ;
        }

        if (reply.getInstanceId() != ctx.instanceId()) {
            if (isDebug()) {
                logger.info("ignore prepare reply({}), current proposal({})", reply.toString(), ctx.toString());
            }

            return ;
        }

    }

    @Override
    public void process(ProposeReply reply) {
        Objects.requireNonNull(reply, "reply");
    }

    @Override
    public void close() {
        ByteBuf value = ctx.value();
        if (value != null) {
            value.release();
        }

        if (task != null) {
            if (!task.isCancelled()) {
                task.cancel(false);
            }
        }
    }

    @Override public boolean isClosed() {
        return false;
    }
}
