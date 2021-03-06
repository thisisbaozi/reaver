package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.BootCommands;
import io.messaginglabs.reaver.com.msg.CommitValue;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.com.msg.Proposing;
import io.messaginglabs.reaver.com.msg.Reconfigure;
import io.messaginglabs.reaver.config.PaxosConfig;
import io.messaginglabs.reaver.config.ConfigEventsListener;
import io.messaginglabs.reaver.config.ConfigView;
import io.messaginglabs.reaver.config.Member;
import io.messaginglabs.reaver.config.MetadataStorage;
import io.messaginglabs.reaver.core.Acceptor;
import io.messaginglabs.reaver.core.Applier;
import io.messaginglabs.reaver.core.DefaultInstanceCache;
import io.messaginglabs.reaver.core.Defines;
import io.messaginglabs.reaver.core.FollowContext;
import io.messaginglabs.reaver.core.InstanceCache;
import io.messaginglabs.reaver.core.Learner;
import io.messaginglabs.reaver.config.GroupConfigs;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.core.Opcode;
import io.messaginglabs.reaver.core.PaxosInstance;
import io.messaginglabs.reaver.core.IdSequencer;
import io.messaginglabs.reaver.core.Proposal;
import io.messaginglabs.reaver.core.Proposer;
import io.messaginglabs.reaver.core.Sequencer;
import io.messaginglabs.reaver.core.Value;
import io.messaginglabs.reaver.core.ValueType;
import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.dsl.GroupStatistics;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.messaginglabs.reaver.utils.ContainerUtils;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiPaxosGroup implements InternalPaxosGroup {

    private static final Logger logger = LoggerFactory.getLogger(MultiPaxosGroup.class);

    // global unique identifier
    private final int id;
    private PaxosGroup.State state;
    private PaxosGroup.Role role;
    private GroupContext ctx;
    private final GroupEnv env;
    private final GroupOptions options;

    private String msg;

    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;
    private Applier applier;
    private StateMachine sm;

    // config
    private final GroupConfigs configs;
    private final Sequencer sequencer;

    // cache
    private final InstanceCache cache;

    private Runnable closeListener;

    public MultiPaxosGroup(int id, StateMachine sm,  GroupEnv env, GroupOptions options) {
        this.env = Objects.requireNonNull(env, "env");
        this.options = Objects.requireNonNull(options, "options");
        this.sm = Objects.requireNonNull(sm, "stateMachine");

        this.id = id;
        this.state = State.NOT_STARTED;
        this.role = Role.UNKNOWN;

        MetadataStorage storage = null;
        if (options.reserveConfig) {
            storage = env.metadataStorage;
        } else {
            /*
             * Disabling storing config does not affect the correctness of
             * Paxos
             */
            logger.info("group({}) do not store config", id);
        }

        this.ctx = new PaxosGroupCtx();
        this.sequencer = new IdSequencer();
        this.configs = null;
        this.cache = new DefaultInstanceCache(1024 * 4, Defines.CACHE_MAX_CAPACITY);
    }

    @Override
    public final State state() {
        return state;
    }

    public final int id() {
        return id;
    }

    @Override
    public final Role role() {
        return role;
    }

    @Override
    public void join(List<Node> members) {
        synchronized (configs) {
            initParticipants();

            if (role == Role.FOLLOWER) {
                becomeFormal();
                return ;
            }

            if (state != State.NOT_STARTED) {
                throw new IllegalStateException(
                    String.format("this node has already joined the group(%d)", id)
                );
            }

            if (configs.size() == 0) {
                /*
                 * either this node is the first one or it joins the group through
                 * the given seed nodes.
                 */
                if (isBoot(members)) {
                    joinAsBoot();
                } else {
                    boolean sent = false;
                    for (Node member : members) {
                        if (!member.equals(local())) {
                            if (join(member)) {
                                logger.info("try to join the group({}) through the member({})", id, member.toString());
                                sent = true;
                                break;
                            }
                        }
                    }

                    if (!sent) {
                        throw new IllegalStateException(String.format(
                            "can't join the group(%d) via seed nodes(%s)",
                            id,
                            ContainerUtils.toString(members, "members")
                        ));
                    }
                }
            } else {
                doJoin();
            }

            state = State.RUNNING;
            role = Role.FORMAL;
        }
    }

    private void doJoin() {

    }

    private void becomeFormal() {

    }

    private void initParticipants() {

    }

    private boolean isBoot(List<Node> members) {
        if (members == null || members.isEmpty()) {
            return true;
        }

        if (members.size() == 1) {
            if (members.get(0).equals(local())) {
                return true;
            }
        }

        return false;
    }


    private void joinAsBoot() {
        if (configs.size() > 0) {
            throw new IllegalStateException("there's already some configurations");
        }

        List<Member> members = new ArrayList<>();
        Member member = new Member();
        member.setMaxVersion(Defines.MAX_VERSION_SUPPORTED);
        member.setMinVersion(Defines.MIN_VERSION_SUPPORTED);
        member.setPort(options.node.getPort());
        member.setIp(options.node.getIp());
        members.add(member);

        PaxosConfig config = configs.build(0, 0, members);
        if (config == null) {
            throw new IllegalStateException("buggy, can't build first config");
        }
        applyConfig(config);

        // reset the instance id
        resetSequencer();
    }

    private void applyConfig(PaxosConfig config) {
        Objects.requireNonNull(config, "config");

        logger.info("apply new config(begin({}), src({}) members({})) to group({})", config.begin(), config.instanceId(), Arrays.toString(config.members()), id);

        if (ctx.maxSeenInstanceId() < config.begin()) {
            if (logger.isInfoEnabled()) {
                logger.info("max instance id this node seen is {}, reset to {}", ctx.maxSeenInstanceId(), config.begin());
            }

            ctx.maxSeenInstanceId(config.begin());
        }

        configs.add(config);
    }

    private void resetSequencer() {
        long beginInstanceId = 0;
        if (ctx.maxSeenInstanceId() == 0) {
            beginInstanceId = 1;
        }

        sequencer.set(beginInstanceId);

        logger.info("resets the begin instance id to {} for group({})", beginInstanceId, id);
    }

    private Member current() {
        Member member = new Member();
        member.setMaxVersion(Defines.MAX_VERSION_SUPPORTED);
        member.setMinVersion(Defines.MIN_VERSION_SUPPORTED);
        member.setPort(options.node.getPort());
        member.setIp(options.node.getIp());
        return member;
    }

    private boolean join(Node node) {
        /*
         * connect with the given current and send it a message that this current
         * wants to join the group
         */
        long timeout = 3000;
        Server server = env.connector.connect(node.getIp(), node.getPort());

        try {
            if (!server.connect(timeout)) {
                logger.info("can't connect with node({})", node.toString());
                return false;
            }

            assert (server.isActive());

            Reconfigure reconfigure = new Reconfigure();
            reconfigure.setOp(Opcode.JOIN_GROUP);
            reconfigure.setMembers(ContainerUtils.toList(current()));
            reconfigure.setGroupId(id);
            server.send(reconfigure, timeout);

            return true;
        } catch (InterruptedException | TimeoutException cause) {
            logger.info("can't join the group({}) through node({})", id, node.toString());
        } finally {
            server.release();
        }

        return false;
    }

    @Override
    public long maxInstanceId(long instanceId) {
        return 0;
    }

    @Override
    public long maxInstanceId() {
        return 0;
    }

    @Override
    public GroupOptions options() {
        return options;
    }

    @Override
    public boolean isSlowDown(long instanceId) {
        /*
         * There's only one factor we need to consider currently:
         *
         * 0. too many finished instances in cache(Applier is too slow)
         */
        return false;
    }

    @Override
    public void freeze(String msg) {
        this.state = State.FROZEN;
        this.msg = msg;
    }

    @Override
    public int pendingCompletedInstances() {
        return 0;
    }

    @Override
    public InstanceCache cache() {
        return cache;
    }

    @Override
    public Node local() {
        return options.node;
    }

    private void checkState() {
        if (state != State.RUNNING) {
            throw new IllegalStateException(
                String.format("group(%d) is not running(%s)", id, state.name())
            );
        }

        if (role != Role.FORMAL) {
            throw new IllegalStateException(
                String.format("group(%d) is not a formal member, it's a %s", id, role.name())
            );
        }

        if (proposer == null) {
            throw new IllegalStateException(
                String.format("group(%d/%s) is unable to learn myValue", id, role.name())
            );
        }
    }

    @Override
    public ByteBuf newBuffer(int capacity) {
        checkValueSize(capacity);

        ByteBuf buf = env.allocator.buffer(capacity + Value.HEADER_SIZE);

        // reserve a number of bytes used to save header
        buf.writerIndex(Value.HEADER_SIZE);
        return buf;
    }

    private ByteBuf check(ByteBuf value) {
        Objects.requireNonNull(value, "value");

        if (value.alloc() != env.allocator) {
            throw new IllegalArgumentException(String.format("the ByteBuf is not allocated by the allocator of the group(%d)", id));
        }

        if (value.readableBytes() <= Value.HEADER_SIZE) {
            throw new IllegalArgumentException("empty value is not allowed");
        }

        // write checksum/size/getType
        Value.init(ValueType.APP_DATA, value);

        return value;
    }

    @Override
    public Commit commit(ByteBuf value) {
        checkState();
        return proposer.commit(check(value));
    }

    @Override
    public CommitResult commit(ByteBuf value, Object att) {
        checkState();
        return proposer.commit(check(value), att);
    }

    private void checkValueSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("can't learn empty myValue");
        }

        int max = Value.MAX_SIZE;
        if (size > max) {
            throw new IllegalArgumentException(
                String.format("max(%d), expect(%d)", max, size)
            );
        }
    }

    private ByteBuf wrapValue(ByteBuffer value) {
        return wrapValue(value, ValueType.APP_DATA);
    }

    private ByteBuf wrapValue(ByteBuffer value, ValueType type) {
        checkValueSize(value.remaining());
        ByteBuf buf = env.allocator.buffer(value.remaining() + Value.HEADER_SIZE);
        return Value.init(type, value, buf);
    }

    @Override
    public Commit commit(ByteBuffer value) {
        checkState();
        return proposer.commit(wrapValue(value));
    }

    @Override
    public CommitResult commit(ByteBuffer value, Object att) {
        checkState();
        return proposer.commit(wrapValue(value), att);
    }

    @Override
    public GroupStatistics statistics() {
        return null;
    }

    public GroupEnv env() {
        return env;
    }

    @Override
    public GroupConfigs configs() {
        return configs;
    }

    @Override
    public void process(Message msg) {
        Objects.requireNonNull(msg, "msg");

        int dstId = msg.getGroupId();
        if (dstId != id) {
            throw new IllegalStateException(
                String.format("buggy, group(%d) can't process messages belong to group(%d), msg(%s)", id, dstId, msg.toString())
            );
        }

        if (state != State.RUNNING) {
            if (logger.isDebugEnabled()) {
                logger.debug("group({}) ignores message({}), it's not running({})", id, msg.toString(), state.name());
            }

            return ;
        }

        dispatch(msg);
    }

    private boolean recovered = false;

    private void bootIfNecessary(long from) {
        if (from != local().id() && !recovered) {
            BootCommands commands = new BootCommands();
            commands.setGroupId(id);
            commands.setOp(Opcode.NEED_BOOT);

            // todo: reply
        }
    }

    private void dispatch(Message msg) {
        if (isConfig(msg.getOp())) {
            processConfig((Reconfigure)msg);
        } else if (isLearner(msg)) {
            processLearner(msg);
        } else if (isAcceptor(msg)) {
            processAcceptor((Proposing)msg);
        } else if (isProposer(msg)) {
            proposer.process(msg);
        } else if (isBootstrap(msg.getOp())) {

        }
    }

    private void processAcceptor(Proposing propose) {
        bootIfNecessary(propose.getNodeId());

        /*
         * before processing the given proposal, two things needs to check:
         *
         * 0. this node is the member of the config
         * 1. this node is a FORMAL participant
         */
        PaxosConfig config = configs.match(propose.getInstanceId());
        if (config == null) {
            if (logger.isDebugEnabled()) {
                logger.warn(
                    "acceptor in group({}) fall behind(current config={}), ignore message(instance={}) from proposer({})",
                    id,
                    configs.current().begin(),
                    propose.getInstanceId(),
                    AddressUtils.toString(propose.getNodeId())
                );
            }

            return ;
        }

        if (role != Role.FORMAL) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "acceptor({}) in group({}) can't vote for any proposal, ignore proposal({})",
                    role.name(),
                    id,
                    propose.getInstanceId()
                );
            }

            return ;
        }

        AcceptorReply reply = acceptor.process(propose);
        if (reply != null) {
            long proposerId = propose.getNodeId();
            if (logger.isTraceEnabled()) {
                logger.trace(
                    ""
                );
            }

            reply(proposerId, config, reply);
        }
    }

    private void createSnapshot(PaxosConfig config, long nodeId) {

    }

    private boolean isBootstrap(Opcode op) {
        return op.isNeedBoot();
    }

    private boolean isProposer(Message msg) {
        return msg.isEmptyPrepareReply()
            || msg.isRefuseAcceptProposal()
            || msg.isPrepareReply()
            || msg.isPromiseAcceptProposal();
    }

    private void reply(long nodeId, PaxosConfig config, AcceptorReply reply) {
        Objects.requireNonNull(reply, "reply");

        /*
         * process the reply directly if the proposer is local
         */
        if (isLocal(nodeId)) {
            process(reply);
        } else {
            /*
             * either this proposer is a member of the config, or it's a independent
             * node.
             */
            Server server = config.find(nodeId);
            if (server != null) {
                server.send(reply);
            } else {
                // todo: it's a independent proposer
            }
        }
    }

    private boolean isLocal(long nodeId) {
        return local().id() == nodeId;
    }

    private boolean isAcceptor(Message msg) {
        return msg.isPrepare() || msg.isPropose();
    }

    private boolean isConfig(Opcode op) {
        return op.isJoinGroup();
    }

    private boolean isLearner(Message msg) {
        return msg.getOp() == Opcode.COMMIT
            || msg.getOp().isLearnChosenValue();
    }

    private void processLearner(Message msg) {
        if (msg.getOp() == Opcode.COMMIT) {
            choose((CommitValue)msg);
        }
    }

    private void choose(CommitValue msg) {
        long instanceId = msg.getInstanceId();

        if (env.debug) {
            logger.debug(
                "starts to choose the value({}/{}) of instance({})",
                msg.getSequence(),
                AddressUtils.toString(msg.getNodeId()),
                instanceId
            );
        }

        PaxosInstance instance = cache.get(msg.getInstanceId());
        if (instance == null) {
            if (env.debug) {
                /*
                 * Likely, the acceptor in this node didn't vote for the Paxos
                 * instance, this node needs to read the value from others in the config.
                 */
                logger.debug(
                    "can't find the instance({}), learn the value from members in the config",
                    msg.getInstanceId()
                );
            }

            learnChosenValue(msg.getInstanceId());
            return ;
        }

        Proposal proposal = instance.acceptor();
        if (proposal.compare(msg.getSequence(), msg.getNodeId()).isEquals()) {
            learnValue(instance, proposal);
        } else {
            /*
             * the value proposer has chosen is not resolved based on the ballot
             * voted by the acceptor in this node, it has to learn it.
             */
            if (env.debug) {

            }

            learnChosenValue(instanceId);
        }
    }

    private void learnChosenValue(long instanceId) {

    }

    private void learnValue(PaxosInstance instance, Proposal proposal) {
        if (env.debug) {

        }

        if (instance.hasChosen()) {
            if (env.debug) {

            }
        } else {
            instance.choose(proposal);
        }

        applyResolvedInstances();
    }

    private long executeCheckpoint = 1;

    private PaxosInstance getSequentialChosenInstances(long instanceId) {
        PaxosInstance instance = cache.get(instanceId);
        if (instance != null && instance.isChosen()) {
            return instance;
        }

        // Do we need to learn chosen values from other nodes?

        return null;
    }

    private void applyResolvedInstances() {
        PaxosInstance instance = getSequentialChosenInstances(executeCheckpoint);
        if (instance == null) {
            return ;
        }

        Proposal chosen = instance.chosen();
        if (chosen == null) {
            throw new IllegalStateException("buggy, no chosen myValue");
        }

        // parse getType
        ValueType type = parseType(chosen.getValue());
        if (isReconfigure(type)) {
            applyReconfiguration(type, instance);
        }

        applyValue(type, instance);
    }

    public boolean isAppData(ValueType type) {
        return type.isAppData();
    }

    public boolean isReconfigure(ValueType type) {
        return type.isJoin() || type.isLeave();
    }

    private ValueType parseType(ByteBuf chosenValue) {
        int offset = 24;    // skip Pxaos properties
        offset += 4;        // skip checksum

        int header = chosenValue.getInt(chosenValue.readerIndex() + offset);
        ValueType type = Value.parse(header);
        if (type == null) {
            throw new IllegalStateException(
                String.format("buggy, can't parse the getType of myValue from header(%d)", header)
            );
        }

        return type;
    }

    private void applyReconfiguration(ValueType type, PaxosInstance instance) {
        // the instance id must greater than current config
        PaxosConfig config = configs.current();
        if (instance.id() <= config.instanceId()) {
            if (logger.isWarnEnabled()) {
                logger.warn(
                    "ignore reconfiguration({}), the instance id should be greater than current config({})",
                    instance.id(),
                    config.instanceId()
                );
            }
            return ;
        }

        reconfigure(type, instance);
    }

    private void reconfigure(ValueType type, PaxosInstance instance) {
        /*
         * 0. apply new configuration
         * 1. clear useless configurations
         */
        Proposal proposal = instance.chosen();
        if (proposal == null) {
            throw new IllegalStateException(String.format("no chosen proposal in instance(%d)", instance.id()));
        }

        if (type.isJoin()) {
            ByteBuf value = instance.chosen().getValue();
            value.readerIndex(value.readerIndex() + 28 + 4);
            Reconfigure reconfigure = Message.decode(value);

            Member[] active = configs.current().members();
            List<Member> members = new ArrayList<>(active.length + 1);
            members.add(reconfigure.getMembers().get(0));
            Collections.addAll(members, active);

            long instanceId = instance.id();
            long beginId = getBeginId(instanceId);

            applyConfig(configs.build(instanceId, beginId, members));
        }
    }

    private long getBeginId(long instanceId) {
        return instanceId + options.pipeline;
    }

    private void applyValue(ValueType type, PaxosInstance instance) {
        if (isAppData(type)) {
            applier.add(instance);
        }
    }

    private void processConfig(Reconfigure msg) {
        // checks whether or not this current is able to process this request
        if (role != Role.FORMAL) {
            throw new IllegalStateException(
                String.format("current in group(%d) is %s, it can't process reconfigure event(%s)", id, role.name(), msg.getOp().name())
            );
        }

        if (msg.getOp().isJoinGroup()) {
            memberJoin(msg);
        }
    }

    private void memberJoin(Reconfigure msg) {
        List<Member> members = msg.getMembers();

        if (logger.isInfoEnabled()) {
            logger.info("a member({}) try to join this group({})", ContainerUtils.toString(members, "members"), id);
        }

        if (members.size() != 1) {
            throw new IllegalStateException(
                String.format("a reconfiguration process one member, but the size of members is %d", members.size())
            );
        }

        ByteBuf buf = msg.encode(env.allocator.buffer(1024));
        try {
            proposer.commit(wrapValue(buf.nioBuffer(), ValueType.MEMBER_JOIN));
        } finally {
            buf.release();
        }
    }

    @Override
    public ConfigView view() {
        return null;
    }

    @Override
    public FollowContext follow(List<Node> nodes) {
        return null;
    }

    @Override
    public Future<Boolean> leave() {
        return null;
    }

    @Override
    public void recommend(Node newLeader) {

    }

    @Override
    public void add(ConfigEventsListener listener) {

    }

    @Override
    public void remove(ConfigEventsListener listener) {

    }

    @Override
    public void close(long timeout) throws Exception {
        synchronized (this) {
            if (isClosed()) {
                return ;
            }

            state = State.SHUTTING_DOWN;
            doClose(timeout);
            state = State.SHUTDOWN;
        }
    }

    private void doClose(long timeout) throws Exception {
        /*
         * 5 things:
         *
         * 0. stop participants
         * 1. shutdown tasks
         * 2. notify state machine
         * 3. releases resources
         * 4. invoke listener if any
         */
        stopParticipants(timeout);
        shutdownTasks();

        try {
            sm.onGroupClosed();
        } catch (Exception cause) {
            logger.warn("caught unknown exception while notifying state machine", cause);
        }

        env.connector.release();
        env.storage.release();
        env.transporter.release();

        if (closeListener != null) {
            try {
                closeListener.run();
            } catch (Exception cause) {
                if (logger.isWarnEnabled()) {
                    logger.warn("caught unknown exception while executing close listener", cause);
                }
            }
        }
    }

    private void stopParticipants(long timeout) throws Exception {
        // follower has not proposer and acceptor
        if (proposer != null) {
            if (!proposer.close(timeout) && timeout > 0) {
                logger.warn("can't close proposer({}) in specified time({})", id, timeout);
            }
        }
    }

    private void close(AutoCloseable src, String id) throws Exception {
        logger.info("Starts to close {}", id);
        long begin = System.currentTimeMillis();
        src.close();
        logger.info("{} has been closed, execution time({})", id, System.currentTimeMillis() - begin);
    }

    private void shutdownTasks() {

    }

    @Override
    public void addCloseListener(Runnable runner) {
        this.closeListener = runner;
    }

    @Override
    public StateMachine getStateMachine() {
        return null;
    }

    @Override
    public void destroy() throws Exception {
        synchronized (this) {
            if (isClosed()) {
                close(-1);
            }

            /*
             * 2 things:
             *
             * 0. delete metadata
             * 1. delete log data if we can
             */

            state = State.DESTROYED;
        }
    }

    @Override
    public GroupContext ctx() {
        return ctx;
    }

    @Override
    public Sequencer sequencer() {
        return sequencer;
    }

    @Override
    public boolean isClosed() {
        return state == State.SHUTTING_DOWN || state == State.SHUTDOWN;
    }

    @Override
    public String toString() {
        return null;
    }

}
