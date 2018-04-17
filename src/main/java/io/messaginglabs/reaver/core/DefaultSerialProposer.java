package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.msg.CommitValue;
import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.com.msg.Proposing;
import io.messaginglabs.reaver.config.PaxosConfig;
import io.messaginglabs.reaver.config.GroupConfigs;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSerialProposer extends AlgorithmParticipant implements SerialProposer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSerialProposer.class);

    private final int groupId;
    private final int id;
    private final long localId;
    private final ProposeContext ctx;

    private int timeout = 1000;
    private Limiter limiter;
    private Future<?> future;

    /*
     * the task is response for checking whether or not a proposal is
     * expired
     */
    // private final ScheduledFuture<?> task;
    private final Sequencer sequencer;
    private final InstanceCache cache;
    private final GroupConfigs configs;
    private final ScheduledExecutorService executor;
    private final Proposing msg;

    public DefaultSerialProposer(int groupId,
                                int id,
                                long localId,
                                InstanceCache cache,
                                Sequencer sequencer,
                                GroupConfigs configs,
                                ByteBufAllocator allocator,
                                ScheduledExecutorService executor) {
        ByteBuf buf = allocator.buffer(1024 * 4);
        if (buf == null) {
            throw new IllegalStateException("bad alloc");
        }

        this.groupId = groupId;
        this.id = id;
        this.localId = localId;

        this.sequencer = sequencer;
        this.cache = cache;
        this.configs = configs;
        this.executor = executor;

        this.ctx = new ProposeContext(buf, groupId, localId);
        this.msg = new Proposing();
        this.msg.setGroupId(groupId);
        this.msg.setProposerId(id);
    }

    @Override
    public void init() {
        this.future = executor.scheduleWithFixedDelay(this::process, timeout, timeout, TimeUnit.MILLISECONDS);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setLimiter(Limiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public List<GenericCommit> valueCache() {
        return ctx.valueCache();
    }

    @Override
    public boolean isBusy() {
        return hasValue();
    }

    private void fail(List<GenericCommit> commits, CommitResult result) {
        for (GenericCommit commit : commits) {
            if (commit.isDone() || commit.isCancelled()) {
                // a bug?
                throw new IllegalStateException(
                    "learn should be pending, but it's done or cancelled"
                );
            }

            commit.setFailure(result);
        }
    }

    public boolean setCommits(List<GenericCommit> commits) {
        if (isClosed()) {
            fail(commits, CommitResult.CLOSED_GROUP);
            return false;
        }

        ctx.setCommits(commits);
        if (isDebug()) {
            logger.trace("set a new learn({}) to proposer({}.{})", commits.size(), groupId, id);
        }

        return true;
    }

    @Override
    public void commit(List<GenericCommit> commits) {
        if (!setCommits(commits)) {
            return ;
        }

        /*
         * Now, this proposer will keep to commit a myValue composed of
         * the given commits until:
         *
         * 0. the value is chosen.
         * 1. the group this proposer belongs to is closed(fail commits)
         * 2. there's no a config
         */
        process();
    }

    public enum Result {
        READY,
        NO_VALUE,
        NO_CONFIG,
        ENABLE_THROTTLE,
        PROPOSE_VALUE,
        PROCESS_CHOSEN_VALUE,
        PROPOSE_AGAIN,
        PROPOSING
    }

    public boolean hasValue() {
        return ctx.valueCache().size() > 0;
    }

    public Result process() {
        if (ctx.stage().isReady()) {
            if (!hasValue()) {
                if (isDebug()) {
                    logger.trace("nothing need to msg for proposer({}) of group({})", id, groupId);
                }

                return Result.NO_VALUE;
            }

            Result result = ready();
            if (result != Result.READY) {
                return result;
            }

            if (Defines.isValidInstance(ctx.instanceId())) {
                throw new IllegalStateException("buggy, instance is still void");
            }

            if (isDebug()) {
                logger.trace(
                    "starts to commit the myValue in phase({}), instance({}), proposal({}), commits({}), stage({}), config({})",
                    ctx.phase().name(),
                    ctx.instanceId(),
                    ctx.ballot().toString(),
                    ctx.valueCache().size(),
                    ctx.stage().name(),
                    ctx.config().toString()
                );
            }

            if (ctx.phase() == AlgorithmPhase.TWO_PHASE) {
                accept();
            } else {
                prepare();
            }

            return Result.PROPOSE_VALUE;
        }

        if (ctx.instance().hasChosen()) {
            /*
             * likely, another proposer owns the instance
             */
            onInstanceDone();
            return Result.PROCESS_CHOSEN_VALUE;
        }

        return retryIfExpired();
    }

    public Result retryIfExpired() {
        if (isExpired()) {
            /*
             * possible causes:
             *
             * 0. rejected by a number of acceptors
             * 1. network trouble
             * 2. the myValue in the instance has been chosen, but it's not the myValue
             *    this proposer proposed
             */
            if (isDebug()) {
                logger.debug(
                    "instance({}) is expired({}/{}), proposer({}/{}), members({}), rejected({}), answered({})",
                    ctx.instanceId(),
                    System.currentTimeMillis() - ctx.begin(),
                    timeout,
                    id,
                    groupId,
                    Arrays.toString(ctx.config().members()),
                    ctx.counter().dumpRejected(),
                    ctx.counter().dumpAccepted()
                );
            }

            nextRound();
            return Result.PROPOSE_AGAIN;
        }

        return Result.PROPOSING;
    }

    public boolean isExpired() {
        assert (hasValue());
        assert (ctx.stage() != PaxosStage.READY);
        assert (ctx.begin() > 0);
        assert (ctx.instanceId() != Defines.VOID_INSTANCE_ID);

        long duration = System.currentTimeMillis() - ctx.begin();
        return duration > timeout;
    }

    public void accept() {
        assert (hasValue());
        assert (ctx.instanceId() != Defines.VOID_INSTANCE_ID);
        assert (ctx.stage().isReady());

        ctx.acceptCounter().reset();
        ctx.ballot().setNodeId(localId);
        ctx.ballot().setSequence(0);
        ctx.setCurrent(0, localId, ctx.myValue());
        ctx.begin(PaxosStage.ACCEPT);

        commit();
    }

    public PaxosConfig find(long instanceId) {
        inLoop();

        if (instanceId < 0) {
            throw new IllegalArgumentException("instance must be 0 or positive number, but given: " + instanceId);
        }

        return configs.match(instanceId);
    }

    public Result ready() {
        assert (hasValue());
        assert (ctx.stage().isReady());

        /*
         * this proposer is able to go ahead unless:
         *
         * 0. match a free instance
         * 1. it's not necessary to slow down
         */
        long instanceId = acquire();

        if (isDebug()) {
            logger.trace("a free instance({}) for proposer({}) of group({})", instanceId, id, groupId);
        }

        if (limiter != null) {
            if (limiter.isEnable(instanceId)) {
                int times = ctx.delay();

                if (isDebug()) {
                    logger.debug("delay({} times) again for instance({}), delay duration({}ms)", times, ctx.instanceId(), ctx.delayed());
                }

                return Result.ENABLE_THROTTLE;
            }
        }

        // match a config based on acquired instance id, if no config
        // is available, fail commits
        PaxosConfig config = find(instanceId);
        if (config == null) {
            if (isDebug()) {
                logger.info("no config is usable for instance({}), fail commits", instanceId);
            }

            fail(CommitResult.NO_CONFIG);
            return Result.NO_CONFIG;
        }

        // init Paxos instance
        PaxosInstance instance = cache.createIfAbsent(instanceId);
        int holder = instance.hold(id);
        if (holder != id) {
            String msg = String.format(
                "proposer(%d) of group(%d) can't hold the instance(%d), it's hold by another one(%d)",
                id,
                groupId,
                instanceId,
                holder
            );
            throw new IllegalStateException(msg);
        }

        sequencer.set(instanceId);
        ctx.reset(instance, config);

        return Result.READY;
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
                logger.info("the myValue of proposer({}) of group({}) is chosen, it's ready to msg next one", id, groupId);
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
            logger.info("span {} instances for allocating a free instance({}) in proposer({}) of group({})", span, instanceId, id, groupId);
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
            processReject(reply, ctx.counter());
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
                groupId,
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
                    groupId,
                    id
                );
            }

            ctx.setAcceptStage();
            ctx.accept().setSequence(ctx.current().getSequence());
            ctx.accept().setNodeId(ctx.current().getNodeId());

            commit();
        }
    }

    private void commit() {
        msg.setSequence(ctx.ballot().getSequence());
        msg.setNodeId(ctx.ballot().getNodeId());
        msg.setValue(ctx.current().getValue());
        msg.setInstanceId(ctx.instanceId());
        msg.setOp(Opcode.ACCEPT);
        msg.setType(Proposing.Type.NORMAL);

        broadcast(msg);
    }

    private void prepare() {
        int sequence = ctx.ballot().getSequence();
        int greatest = ctx.getGreatestSeen().getSequence();
        if (sequence <= greatest) {
            sequence = greatest;
            sequence++;
        }

        ctx.delay();

        msg.setSequence(sequence);
        msg.setNodeId(localId);
        msg.setValue(ctx.current().getValue());
        msg.setInstanceId(ctx.instanceId());
        msg.setType(Proposing.Type.NORMAL);
        msg.setOp(Opcode.PREPARE);

        // reset the time of beginning
        ctx.begin(PaxosStage.PREPARE);
        ctx.setCurrent(sequence, localId, ctx.current().getValue());

        broadcast(msg);
    }

    private void broadcast(Message msg) {
        for (Server server : ctx.config().servers()) {
            server.send(msg);
        }
    }

    private boolean isIgnore(long instanceId, AcceptorReply reply) {
        Objects.requireNonNull(reply, "reply");

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
                    groupId,
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
                groupId,
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

        BallotsCounter counter = ctx.acceptCounter();
        if (reply.isRefuseAcceptProposal()) {
            processReject(reply, counter);
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
                        groupId
                    );
                }

                chooseValue();
                ctx.setChooseBallot(ctx.ballot());
            }
        } else {
            throw new IllegalStateException("buggy, unknown getOp: " + reply.getOp().name());
        }
    }

    private void processReject(AcceptorReply reply, BallotsCounter counter) {
        counter.countRejected(reply.getAcceptorId());

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
                groupId
            );
        }
        ctx.setGreatestSeen(reply.getNodeId(), reply.getSequence());

        int majority = ctx.config().majority();
        int count = counter.nodesRejected();
        if (count > majority) {
            if (isDebug()) {
                logger.debug(
                    "a majority of acceptors({}/{}) have rejected proposal({}) from this proposer({}/{})",
                    majority,
                    ctx.config().members().length,
                    ctx.instanceId(),
                    id,
                    groupId
                );
            }

            nextRound();
        }
    }

    private void nextRound() {
        ctx.counter().reset();
        ctx.acceptCounter().reset();
        ctx.setPhase(AlgorithmPhase.THREE_PHASE);

        prepare();
    }

    private void chooseValue() {
        CommitValue msg = new CommitValue();
        msg.setInstanceId(ctx.instanceId());
        msg.setNodeId(ctx.ballot().getNodeId());
        msg.setSequence(ctx.ballot().getSequence());
        msg.setGroupId(groupId);
        msg.setOp(Opcode.COMMIT);

        ByteBuf value = ctx.current().getValue();
        if (value == null || value.readableBytes() == 0) {
            // msg.setType(Message.Type.EMPTY_OP);
        }

        if (isDebug()) {
            long proposer = ctx.current().getNodeId();
            logger.debug(
                "choose value({}) proposed by {} for instance({}), acceptors({})",
                (value == null ? "empty" : value.readableBytes()),
                (proposer == localId ? "local" : AddressUtils.toString(proposer)),
                ctx.instanceId(),
                ctx.acceptCounter().dumpAccepted()
            );
        }

        broadcast(msg);
    }

    public void disableTimeoutCheck() {
        if (future != null) {
            if (!future.isCancelled()) {
                future.cancel(false);
            }
        }
    }

    public ProposeContext ctx() {
        return ctx;
    }

    @Override
    public void close() {
        disableTimeoutCheck();

        ByteBuf value = ctx.myValue();
        if (value != null) {
            value.release();
        }
    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
