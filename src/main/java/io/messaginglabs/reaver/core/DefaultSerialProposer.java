package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.com.msg.PrepareReply;
import io.messaginglabs.reaver.com.msg.ProposeReply;
import io.messaginglabs.reaver.config.Config;
import io.messaginglabs.reaver.config.GroupConfigs;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.dsl.Group;
import io.messaginglabs.reaver.group.GroupEnv;
import io.messaginglabs.reaver.group.PaxosGroup;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSerialProposer extends AlgorithmParticipant implements SerialProposer {

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
        task = env.executor.scheduleWithFixedDelay(this::process, interval, interval, TimeUnit.MILLISECONDS);
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
        process();
    }

    private void process() {
        if (group.state() != Group.State.RUNNING) {
            /*
             * fail commits if any
             */
            fail(CommitResult.CLOSED_GROUP);
            return ;
        }

        if (ctx.currentPhase().isReady()) {
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

            start(ctx);
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
                    ctx.counter().dumpPromised()
                );
            }

            if (ctx.isRefused()) {
                // refused by some one, propose the value based on 3 phases
                // proposeIn3Phase(ctx);
            } else {
                // timeout?
                // proposeIn2Phase(ctx);
            }
        }
    }

    public Config find(long instanceId) {
        inLoop();

        if (instanceId < 0) {
            throw new IllegalArgumentException("instance must be 0 or positive number, but given: " + instanceId);
        }

        GroupConfigs configs = group().configs();
        if (configs == null) {
            throw new IllegalStateException(
                String.format("no configs in group(%s)", group().id())
            );
        }

        return configs.find(instanceId);
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
        // propose();
    }

    private AlgorithmPhase getPhase() {
        if (group.env().phase == AlgorithmPhase.THREE_PHASE) {
            return AlgorithmPhase.THREE_PHASE;
        }

        return ctx.phase();
    }

    private void start(ProposeContext ctx) {
        validate(ctx);

        if (!ctx.currentPhase().isReady()) {
            throw new IllegalStateException(
                String.format("buggy, ctx should be ready currentPhase, but it's %s", ctx.currentPhase().name())
            );
        }

        Proposal proposal = ctx.proposal();
        if (proposal == null) {
            throw new IllegalStateException("buggy, can't create proposal");
        }

        Config config = ctx.config();
        AlgorithmPhase phase = getPhase();
        long instanceId = ctx.instanceId();

        if (isDebug()) {
            logger.trace(
                "starts to propose the value in phase({}), instance({}), proposal({}), commits({}), currentPhase({}), config({})",
                phase.name(),
                instanceId,
                proposal.toString(),
                ctx.valueCache().size(),
                ctx.currentPhase().name(),
                config.toString()
            );
        }

        PaxosPhase stage;
        if (getPhase() == AlgorithmPhase.TWO_PHASE) {
            config.propose(instanceId, proposal);
            stage = ctx.setStage(PaxosPhase.ACCEPT);
        } else {
            config.prepare(instanceId, proposal);
            stage = ctx.setStage(PaxosPhase.PREPARE);
        }

        if (!stage.isReady()) {
            // a bug
            throw new IllegalStateException(
                String.format("buggy, the currentPhase of context should be ready, but it's %s", stage.name())
            );
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
        if (isIgnorable(reply.getInstanceId(), PaxosPhase.PREPARE, reply)) {
            return ;
        }

        VotersCounter counter = ctx.counter();
        if (reply.getOp() == Message.Operation.REJECT_PREPARE) {
            if (isDebug()) {
                logger.trace(
                    "acceptor({}) rejected proposer({}/{})'s proposal(sequence={}, instance={}), due to an larger proposal({}/{})",
                    AddressUtils.toString(reply.getAcceptor()),
                    id,
                    group.id(),
                    ctx.proposal().sequence,
                    ctx.instanceId(),
                    reply.getSequence(),
                    AddressUtils.toString(reply.getNodeId())
                );
            }

            counter.countRejected(reply.getAcceptor());

            // update view
            ctx.setLargerSequence(reply.getNodeId(), reply.getSequence());
        } else if (reply.isPrepareReply() || reply.isEmptyReply()) {
            if (isDebug()) {
                logger.trace(
                    "acceptor({}) accept proposer({}/{})'s proposal(sequence={}, instance={})",
                    AddressUtils.toString(reply.getAcceptor()),
                    id,
                    group.id(),
                    ctx.proposal().sequence,
                    ctx.instanceId()
                );
            }

            counter.countPromised(reply.getAcceptor());

            if (reply.isPrepareReply()) {
                /*
                 * this acceptor made this reply has accepted a value, propose
                 * other's value first if its proposal is greater
                 */
                boolean isSmaller = ctx.proposal().compare(reply.getSequence(), reply.getNodeId()).isGreater();
                if (isSmaller) {
                    ByteBuf value = reply.getValue();
                    if (value == null) {
                        throw new IllegalStateException("buggy, no value");
                    }

                    ctx.setOtherValue(value);

                    if (isDebug()) {
                        logger.debug(
                            "replace value({}) proposed by other({}) first because this proposer's({}/{}) proposal({}) is smaller than other({})",
                            value.readableBytes(),
                            AddressUtils.toString(reply.getNodeId()),
                            id,
                            group.id(),
                            ctx.proposal().toString(),
                            reply.getSequence()
                        );
                    }
                }
            }
        } else {
            throw new IllegalStateException(
                "unknown op: " + reply.getOp().name()
            );
        }

        checkPrepareResult(counter);
    }

    private void checkPrepareResult(VotersCounter counter) {
        /*
         * three state:
         *
         * 0. accepted(accepted by a majority of members)
         * 1. rejected
         * 2. pending(do nothing)
         */
        Config config = ctx.config();
        int majority = ctx.config().majority();
        if (counter.nodesPromised() >= majority) {
            if (isDebug()) {
                logger.trace(
                    "the prepare phase of proposal({}) of proposer({}/{}) is finished, go to the next phase",
                    ctx.proposal().toString(),
                    id,
                    group.id()
                );
            }

            ctx.setStage(PaxosPhase.ACCEPT);

            // It's no necessary to process replies for first phase when we get here
            // clear votes.
            counter.reset();
            config.propose(ctx.instanceId(), ctx.proposal());
        } else if (counter.nodesRejected() >= majority) {
            if (isDebug()) {
                logger.trace(
                    "the proposal({}) of proposer({}/{}) is rejected, go to the next phase",
                    ctx.proposal().toString(),
                    id,
                    group.id()
                );
            }
        }
    }

    private boolean isIgnorable(long instanceId, PaxosPhase phase, Message reply) {
        Objects.requireNonNull(reply, "reply");

        if (ctx.instance() == null) {
            if (isDebug()) {
                logger.info("ignore reply({}), can't find a instance associated with it", reply.toString());
            }

            return true;
        }

        if (instanceId != ctx.instanceId()) {
            if (isDebug()) {
                logger.info("ignore prepare reply({}), current proposal({})", reply.toString(), ctx.toString());
            }

            return true;
        }

        if (ctx.currentPhase() != phase) {
            if (isDebug()) {
                logger.info(
                    "ctx(instance={}, currentPhase={}) is not in Paxos PREPARE currentPhase, ignore prepare reply({})",
                    ctx.instanceId(),
                    ctx.currentPhase().name(),
                    reply.toString()
                );
            }
        }

        return false;
    }


    @Override
    public void process(ProposeReply reply) {
        if (isIgnorable(reply.getInstanceId(), PaxosPhase.ACCEPT, reply)) {
            return ;
        }

        VotersCounter counter = ctx.counter();
        if (reply.isAcceptRejected()) {
            if (isDebug()) {

            }

            counter.countRejected(reply.getAcceptorId());
        } else if (reply.isAccepted()) {
            if (isDebug()) {

            }
            counter.countPromised(reply.getAcceptorId());
        } else {
            throw new IllegalStateException(
                "unknown msg reply type: " + reply.op().name()
            );
        }

        checkAcceptPhase(counter);
    }

    private void checkAcceptPhase(VotersCounter counter) {
        int majority = ctx.config().majority();
        if (counter.nodesPromised() >= majority) {
            if (isDebug()) {

            }

            /*
             * now, the value in the instance this proposer is proposing
             * is chosen, commit it.
             */
            onInstanceDone();
        } else if (counter.nodesRejected() >= majority) {
            if (isDebug()) {

            }
        }
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

    @Override
    public boolean isClosed() {
        return false;
    }
}
