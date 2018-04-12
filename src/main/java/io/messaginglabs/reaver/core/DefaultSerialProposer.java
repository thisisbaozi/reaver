package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.msg.LearnValue;
import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Message;
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
    private final Propose msg;

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

        this.ctx = new ProposeContext(buf, group.id(), group.local().id());
        this.msg = new Propose();
        this.msg.setGroupId(group.id());
        this.msg.setProposerId(id);

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
            logger.trace("set a new commit({}) to proposer({}.{})", commits.size(), group.id(), id);
        }

        /*
         * Now, this proposer will keep to propose a myValue composed of
         * the given commits until:
         *
         * 0. the myValue is chosen.
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
                    logger.trace("nothing need to msg for proposer({}) of group({})", id, group.id());
                }

                return ;
            }

            if (!readyToRun()) {
                return ;
            }

            if (Defines.isValidInstance(ctx.instanceId())) {
                throw new IllegalStateException("buggy, instance is still void");
            }

            if (isDebug()) {
                logger.trace(
                    "starts to propose the myValue in phase({}), instance({}), proposal({}), commits({}), currentPhase({}), config({})",
                    ctx.phase().name(),
                    ctx.instanceId(),
                    ctx.ballot().toString(),
                    ctx.valueCache().size(),
                    ctx.currentPhase().name(),
                    ctx.config().toString()
                );
            }

            if (ctx.phase() == AlgorithmPhase.TWO_PHASE) {
                proposeWithoutPreparing();
            } else {
                prepare();
            }

            return ;
        }

        if (ctx.instance().hasChosen()) {
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
             * 2. the myValue in the instance has been chosen, but it's not the myValue
             *    this proposer proposed
             */
            if (isDebug()) {
                logger.info(
                    "msg instance({}) is expired, proposer({}/{}), ratio({}/{}), answered({})",
                    ctx.instanceId(),
                    id,
                    group.id(),
                    "",
                    ctx.config().members().length,
                    ctx.counter().dumpAccepted()
                );
            }

            doNextRound();
        }
    }

    private void proposeWithoutPreparing() {
        ctx.acceptCounter().reset();

        ctx.ballot().setNodeId(group.local().id());
        ctx.ballot().setSequence(0);
        ctx.setCurrent(0, group.local().id(), ctx.myValue());

        propose();
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
        ctx.reset(instanceId, config);

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
         * ths Paxos instance is done, but the myValue in this instance may
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
                logger.info("the myValue of proposer({}) of group({}) is chosen, it's ready to msg next one", id, group.id());
            }

            return ;
        }

*/
        /*
         * it's not the myValue we proposed, try again in a new instance
         */
        // msg();
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
        if (isIgnore(reply.getInstanceId(), reply)) {
            return ;
        }

        if (reply.isRejectPrepare()) {
            processReject(reply);
            return ;
        }

        ctx.counter().countPromised(reply.getAcceptorId());

        Proposal current = ctx.current();
        int sequence = current.getSequence();
        long nodeId = current.getNodeId();

        boolean newValue = ctx.setCurrent(reply.getSequence(), reply.getNodeId(), reply.getValue());
        if (newValue && isDebug()) {
            logger.debug(
                "proposer({}.{}) seen a newer myValue({}/{}) for instance({}) from acceptor({}), replace old one({}/{})",
                group.id(),
                id,
                reply.getSequence(),
                reply.getNodeId(),
                reply.getInstanceId(),
                AddressUtils.toString(reply.getAcceptorId()),
                sequence,
                nodeId
            );
        }

        if (ctx.ballot().compare(ctx.accept()).isGreater()) {
            proposeIfEnough();
        }
    }

    private void proposeIfEnough() {
        int majority = ctx.config().majority();
        int count = ctx.counter().nodesPromised();
        if (count > majority) {
            if (isDebug()) {
                logger.trace(
                    "acceptors({}/{}) have accepted the prepare proposal({}/{}) of proposer({}.{}, msg it)",
                    count,
                    ctx.config().total(),
                    ctx.current().getSequence(),
                    ctx.current().getNodeId(),
                    group.id(),
                    id
                );
            }

            ctx.acceptCounter().reset();
            ctx.accept().setSequence(ctx.current().getSequence());
            ctx.accept().setNodeId(ctx.current().getNodeId());

            propose();
        }
    }

    private void propose() {
        msg.setSequence(ctx.ballot().getSequence());
        msg.setNodeId(ctx.ballot().getNodeId());
        msg.setValue(ctx.current().getValue());
        msg.setInstanceId(ctx.instanceId());
        msg.setOp(Opcode.PROPOSE);

        broadcast(msg);
    }

    private void prepare() {
        int sequence = ctx.ballot().getSequence();
        int greatest = ctx.getGreatestSeen().getSequence();
        if (sequence < greatest) {
            sequence = greatest;
            sequence++;
        }

        msg.setSequence(sequence);
        msg.setNodeId(ctx.ballot().getNodeId());
        msg.setValue(ctx.current().getValue());
        msg.setInstanceId(ctx.instanceId());

        broadcast(msg);
    }

    private void broadcast(Message msg) {
        for (Server server : ctx.config().servers()) {
            server.send(msg);
        }
    }

    private boolean isIgnore(long instanceId, AcceptorReply reply) {
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
        if (isIgnore(reply.getInstanceId(), reply)) {
            return ;
        }

        BallotsCounter counter = ctx.counter();
        if (reply.isRefuseAcceptProposal()) {
            processReject(reply);
        } else if (reply.isPromiseAcceptProposal()) {
            counter.countPromised(reply.getAcceptorId());

            // choose the myValue if a majority of acceptors in the config has
            // promised for the proposal this proposer sent.
            Ballot.CompareResult result = ctx.choose().compare(reply.getSequence(), reply.getNodeId());
            if (result.isSmaller() || result.isGreater()) {
                return ;
            }

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
                ctx.setChooseBallot(ctx.ballot());
            }
        } else {
            throw new IllegalStateException("buggy, unknown op: " + reply.op().name());
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
    }



    private void chooseValue() {
        LearnValue msg = new LearnValue();
        msg.setInstanceId(ctx.instanceId());
        msg.setNodeId(ctx.ballot().getNodeId());
        msg.setSequence(ctx.ballot().getSequence());
        msg.setGroupId(group.id());
        msg.setOp(Opcode.CHOOSE_VALUE);

        ByteBuf value = ctx.current().getValue();
        if (value == null || value.readableBytes() == 0) {
            msg.setType(Message.Type.EMPTY_OP);
        }

        if (isDebug()) {
            long proposer = ctx.current().getNodeId();
            logger.debug(
                "choose value({}) proposed by {} for instance({}), acceptors({})",
                (value == null ? "empty" : value.readableBytes()),
                (proposer == group.local().id() ? "local" : AddressUtils.toString(proposer)),
                ctx.instanceId(),
                ctx.acceptCounter().dumpAccepted()
            );
        }

        broadcast(msg);
    }

    @Override
    public void close() {
        ByteBuf value = ctx.myValue();
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
