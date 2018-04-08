package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.msg.LearnValue;
import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Propose;
import io.messaginglabs.reaver.config.PaxosConfig;
import io.messaginglabs.reaver.config.GroupConfigs;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.group.GroupEnv;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSerialProposer extends AlgorithmParticipant implements SerialProposer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSerialProposer.class);

    private final int id;
    private final ProposeContext ctx;

    /*
     * the task is response for checking whether or not a proposal is
     * expired
     */
    // private final ScheduledFuture<?> task;
    private final Sequencer sequencer;

    public DefaultSerialProposer(int id, InternalPaxosGroup group) {
        super(group);

        GroupEnv env = group.env();
        if (env == null) {
            throw new IllegalArgumentException("no group env");
        }

        ByteBuf buf = env.allocator.buffer(1024 * 4);
        if (buf == null) {
            throw new IllegalStateException("bad alloc");
        }

        this.id = id;
        this.sequencer = group.sequencer();

        this.ctx = new ProposeContext(buf);

        int interval = group.options().retryInterval;
        // this.task = env.executor.scheduleWithFixedDelay(this::process, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public List<GenericCommit> valueCache() {
        return ctx.valueCache();
    }

    @Override
    public boolean isBusy() {
        return ctx.instanceId() != Defines.VOID_INSTANCE_ID;
    }

    private void fail(List<GenericCommit> commits, CommitResult result) {
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
        if (group.state() != PaxosGroup.State.RUNNING) {
            /*
             * fail commits if any
             */
            fail(CommitResult.CLOSED_GROUP);
            return ;
        }

        if (ctx.currentPhase().isReady()) {
            if (ctx.valueCache().isEmpty()) {
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
            /*
             * likely, another proposer owns the instance
             */
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
                    ctx.config().members().length,
                    ctx.counter().dumpPromised()
                );
            }

            doNextRound();
        }
    }

    public PaxosConfig find(long instanceId) {
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

        return configs.match(instanceId);
    }

    private boolean readyToRun() {
        /*
         * this proposer is able to go ahead unless:
         *
         * 0. match a free instance
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

        // match a config based on acquired instance id, if no config
        // is available, fail commits
        PaxosConfig config = find(instanceId);
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

        sequencer.set(instanceId);
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
        //Proposal chosen = ctx.instance().chosen();
        /*
        if (chosen == null) {
            throw new IllegalStateException(
                String.format("buggy, instance(%s) is still pending", ctx.instance().touch())
            );
        }
        */

        /*
         * ths Paxos instance is done, but the value in this instance may
         * not be the one this proposer proposed
         */
        if (isDebug()) {
            logger.info(ctx.dumpChosenInstance());
        }

        /*
        if (chosen.getNodeId() == ctx.nodeId()) {
            // it's enough, group id + current id must be unique
            ctx.clear();

            if (isDebug()) {
                logger.info("the value of proposer({}) of group({}) is chosen, it's ready to propose next one", id, group.id());
            }

            return ;
        }

*/
        /*
         * it's not the value we proposed, try again in a new instance
         */
        // propose();
    }

    private AlgorithmPhase getPhase() {
        return ctx.phase();
    }

    private void start(ProposeContext ctx) {
        validate(ctx);

        PaxosConfig config = ctx.config();
        long instanceId = ctx.instanceId();

        if (isDebug()) {
            logger.trace(
                "starts to propose the value in phase({}), instance({}), proposal({}), commits({}), currentPhase({}), config({})",
                ctx.phase().name(),
                instanceId,
                ctx.ballot().toString(),
                ctx.valueCache().size(),
                ctx.currentPhase().name(),
                config.toString()
            );
        }

        Propose propose = new Propose();
        propose.setNodeId(ctx.ballot().getNodeId());
        propose.setValue(ctx.value());
        propose.setInstanceId(ctx.instanceId());
        propose.setProposerId(id);
        propose.setGroupId(group.id());

        if (getPhase() == AlgorithmPhase.TWO_PHASE) {
            propose.setSequence(ctx.ballot().getSequence());
            propose.setOp(Opcode.PROPOSE);

            ctx.setStage(PaxosPhase.ACCEPT);
        } else {
            propose.setSequence(ctx.maxSequence() + 1);
            propose.setOp(Opcode.PREPARE);

            ctx.setStage(PaxosPhase.PREPARE);
        }

        for (Server server : config.servers()) {
            server.send(propose);
        }
    }



    private void validate(ProposeContext ctx) {
        Objects.requireNonNull(ctx, "ctx");

        if (ctx.config() == null) {
            throw new IllegalStateException(
                String.format("no config in ctx(%s)", ctx.toString())
            );
        }

        if (group.local() == null) {
            throw new IllegalStateException(
                String.format("buggy, current is null in config(%s)", ctx.config().toString())
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
         * match a sequence number for a new ctx, must ensure that
         * no ctx associated with the new sequence.
         */
        InstanceCache cache = group.cache();
        if (cache == null) {
            throw new IllegalStateException(String.format("no cache in group(%d)", group.id()));
        }

        int span = 0;
        long instanceId = sequencer.get();

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
    public void process(AcceptorReply reply) {
        if (reply.isPrepareReply() || reply.isEmptyPrepareReply()) {
            processPrepareReply(reply);
        } else if (reply.isPromiseAcceptProposal() || reply.isRefuseAcceptProposal()) {
            processAcceptReply(reply);
        }
    }

    private void processPrepareReply(AcceptorReply reply) {
        if (isIgnore(reply.getInstanceId(), PaxosPhase.PREPARE, reply)) {
            return ;
        }

        if (reply.isRejectPrepare()) {
            processReject(reply);
            return ;
        }

        ctx.counter().countPromised(reply.getAcceptorId());

        if (reply.isPrepareReply()) {
            /*
             * this acceptor made this reply has acceptor a value, propose
             * other's value first if its proposal is greater
             */
            boolean isSmaller = ctx.ballot().compare(reply.getSequence(), reply.getNodeId()).isGreater();
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
                        ctx.ballot().toString(),
                        reply.getSequence()
                    );
                }
            }
        }

        proposeIfEnough();
    }

    private void proposeIfEnough() {
        /*
         * three state:
         *
         * 0. acceptor(acceptor by a majority of members)
         * 1. rejected
         * 2. pending(do nothing)
         */
        PaxosConfig config = ctx.config();
        // int majority = ctx.config().majority();
        int majority = 0;
        if (ctx.counter().nodesPromised() >= majority) {
            if (isDebug()) {
                logger.trace(
                    "the prepare phase of proposal({}) of proposer({}/{}) is finished, go to the next phase",
                    ctx.ballot().toString(),
                    id,
                    group.id()
                );
            }

            ctx.setStage(PaxosPhase.ACCEPT);

            // It's no necessary to process replies for first phase when we get here
            // clear votes.
            ctx.counter().reset();
            // config.propose(ctx.instanceId(), ctx.proposal());
        } else if (ctx.counter().nodesRejected() >= majority) {
            if (isDebug()) {
                logger.trace(
                    "the proposal({}) of proposer({}/{}) is rejected, go to the next phase",
                    ctx.ballot().toString(),
                    id,
                    group.id()
                );
            }
        }
    }

    private boolean isIgnore(long instanceId, PaxosPhase phase, AcceptorReply reply) {
        Objects.requireNonNull(reply, "reply");

        if (group.role() != PaxosGroup.Role.FORMAL) {
            return true;
        }

        if (instanceId != ctx.instanceId()) {
            if (isDebug()) {
                logger.debug(
                    "ignore reply({}) from acceptor({}), the proposer cares about instance({}), the replay is for instance({})",
                    reply.toString(),
                    AddressUtils.toString(reply.getAcceptorId()),
                    ctx.instanceId(),
                    instanceId
                );
            }

            return true;
        }

        if (ctx.currentPhase() != phase) {
            /*
             * this proposer either starts a new round for the instance or move to
             * the next phase, so ignores these stale replies.
             */
            if (isDebug()) {
                logger.debug(
                    "the proposer({}) of group({}) is in {} phase for instance({}) instead of {}, ignores the stale reply({}) from acceptor({})",
                    id,
                    group.id(),
                    ctx.instanceId(),
                    ctx.currentPhase().name(),
                    phase.name(),
                    reply.toString()
                );
            }

            return true;
        }

        int sequence = ctx.ballot().getSequence();
        if (sequence != reply.getReplySequence()) {
            // it's a stale reply for previous proposals, ignore it
            if (isDebug()) {
                logger.debug(
                    "the reply({}) from acceptor({}) is for proposal({}), but proposer({}/{}) is proposing with {}, ignores it",
                    reply.getInstanceId(),
                    AddressUtils.toString(reply.getAcceptorId()),
                    reply.getReplySequence(),
                    id,
                    group.id(),
                    sequence
                );
            }

            return true;
        }

        PaxosConfig config = find(instanceId);
        if (config == null ) {
            /*
             * Likely, it's impossible...
             */
            throw new IllegalStateException("can't find config for instance: " + instanceId);
        }

        if (!config.isMember(reply.getAcceptorId())) {
            if (isDebug()) {
                logger.debug(
                    "config({}/{}/{}) doesn't contains the acceptor({}), ignores its reply for instance({})",
                    config.toString(),
                    AddressUtils.toString(reply.getAcceptorId()),
                    ctx.instanceId()
                );
            }
            return true;
        }

        if (isDebug()) {
            logger.debug(
                "proposer({}/{}) starts to process reply({}) from acceptor({})",
                id,
                group.id(),
                reply.toString(),
                AddressUtils.toString(reply.getAcceptorId())
            );
        }

        return false;
    }

    private void processAcceptReply(AcceptorReply reply) {
        if (isIgnore(reply.getInstanceId(), PaxosPhase.ACCEPT, reply)) {
            return ;
        }

        BallotsCounter counter = ctx.counter();
        if (reply.isRefuseAcceptProposal()) {
            processReject(reply);
        } else if (reply.isPromiseAcceptProposal()) {
            counter.countPromised(reply.getAcceptorId());

            // choose the value if a majority of acceptors in the config has
            // promised for the proposal this proposer sent.
            Ballot.CompareResult result = ctx.choose().compare(reply.getSequence(), reply.getNodeId());
            if (result.isSmaller() || result.isGreater()) {
                return ;
            }

            chooseIfEnough(counter);
        } else {
            throw new IllegalStateException("buggy, unknown msg reply type: " + reply.op().name());
        }
    }

    private void processReject(AcceptorReply reply) {
        ctx.counter().countRejected(reply.getAcceptorId());

        Ballot ballot = ctx.getGreatestSeen();
        if (isDebug()) {
            logger.debug(
                "acceptor({}) rejected({}/{}) proposal({},{},{}) from proposer({}/{}), current greatest seen({},{}), acceptor promised({},{})",
                AddressUtils.toString(reply.getAcceptorId()),
                ctx.counter().nodesRejected(),
                ctx.config().members().length,
                ctx.ballot().getSequence(),
                ctx.ballot().getNodeId(),
                ctx.instanceId(),
                ballot.getSequence(),
                ballot.getNodeId(),
                reply.getSequence(),
                reply.getNodeId(),
                id,
                group.id()
            );
        }
        ctx.setGreatestSeen(reply.getNodeId(), reply.getSequence());

        int majority = ctx.config().majority();
        int count = ctx.counter().nodesRejected();
        if (count > majority) {
            if (isDebug()) {
                logger.debug(
                    "a majority of acceptors({}/{}) have rejected proposal({}) from this proposer({}/{})",
                    majority,
                    ctx.config().members().length,
                    ctx.instanceId(),
                    id,
                    group.id()
                );
            }

            doNextRound();
        }
    }

    private void doNextRound() {
        ctx.counter().reset();
        ctx.setPhase(AlgorithmPhase.THREE_PHASE);
        start(ctx);
    }

    private void chooseIfEnough(BallotsCounter counter) {
        int majority = ctx.config().majority();
        int count = counter.nodesPromised();
        if (count > majority) {
            if (isDebug()) {
                logger.debug(
                    "a majority of acceptors({}) have promised to instance({}) of proposer({}/{})",
                    majority,
                    ctx.instanceId(),
                    id,
                    group.id()
                );
            }

            chooseValue();
            ctx.setChooseBallot(this.ctx.ballot());
        }
    }

    private void chooseValue() {
        LearnValue msg = new LearnValue();
        msg.setInstanceId(ctx.instanceId());
        msg.setNodeId(ctx.ballot().getNodeId());
        msg.setSequence(ctx.ballot().getSequence());
        msg.setGroupId(group.id());
        msg.setOp(Opcode.LEARN_VALUE);

        ctx.config().broadcast(msg);
    }

    @Override
    public void close() {
        ByteBuf value = ctx.value();
        if (value != null) {
            value.release();
        }

        /*
        if (task != null) {
            if (!task.isCancelled()) {
                task.cancel(false);
            }
        }
        */
    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
