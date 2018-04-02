package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.msg.LearnValue;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.com.msg.Reconfigure;
import io.messaginglabs.reaver.config.PaxosConfig;
import io.messaginglabs.reaver.config.ConfigEventsListener;
import io.messaginglabs.reaver.config.ConfigView;
import io.messaginglabs.reaver.config.Member;
import io.messaginglabs.reaver.config.MetadataStorage;
import io.messaginglabs.reaver.config.PaxosGroupConfigs;
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
import io.messaginglabs.reaver.core.ParallelAcceptor;
import io.messaginglabs.reaver.core.ParallelProposer;
import io.messaginglabs.reaver.core.PaxosInstance;
import io.messaginglabs.reaver.core.Proposer;
import io.messaginglabs.reaver.core.ValueType;
import io.messaginglabs.reaver.core.ValueUtils;
import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.dsl.GroupStatistics;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.messaginglabs.reaver.utils.ContainerUtils;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiPaxosGroup implements InternalPaxosGroup {

    private static final Logger logger = LoggerFactory.getLogger(MultiPaxosGroup.class);

    // global unique identifier
    private final int id;
    private PaxosGroup.State state;
    private PaxosGroup.Role role;

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

        this.configs = new PaxosGroupConfigs(this, storage);
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
            if (state != State.NOT_STARTED) {
                throw new IllegalStateException(
                    String.format("state of group(%d) is %s, can't join again", id, state.name())
                );
            }

            if (role == Role.FOLLOWER) {
                stopFollow();
            }

            Role current = role;
            role = Role.FORMAL;
            logger.info("current({}) try join to the group({}) in role({}), old role({})", local().toString(), id, role.name(), current.name());

            initParticipants();

            PaxosConfig config = configs.newest();
            if (config != null) {
                initCache();
                replay();

                return ;
            }

            if (!doJoin(members)) {
                throw new IllegalStateException(
                    String.format("member(%s) of group(%d) can't join through members(%s)", local().toString(), id, ContainerUtils.toString(members, "members"))
                );
            }

            // Wait until this current joined the group
            waitUntilJoined();
        }
    }

    private void waitUntilJoined() {

    }

    private void stopFollow() {

    }

    private void initCache() {

    }

    private void replay() {

    }

    private void initParticipants() {
        if (role == Role.FORMAL) {
            if (proposer == null) {
                proposer = new ParallelProposer(options.valueCacheCapacity, options.batch, options.pipeline);
            }

            if (acceptor == null) {
                acceptor = new ParallelAcceptor(this);
            }
        }
    }

    private boolean doJoin(List<Node> members) {
        ContainerUtils.checkNotEmpty(members, "members");

        if (logger.isInfoEnabled()) {
            logger.info(
                "this current({}) try to join the group({}) based on the given donors({})",
                local().toString(),
                id,
                ContainerUtils.toString(members, "members")
            );
        }

        for (Node member : members) {
            if (member.equals(local())) {
                continue;
            }

            if (join(member)) {
                logger.info("try to join the group({}) through the member({})", id, member.toString());
                return true;
            }
        }

        return false;
    }

    private boolean join(Node node) {
        /*
         * connect with the given current and send it a message that this current
         * wants to join the group
         */
        Server server = env.connector.connect(node.getIp(), node.getPort());
        // boolean result = server.join(id, local());

        /*
         * if others rely on this server, they should connect with it by themselves
         */
        server.release();

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
        return null;
    }

    @Override
    public boolean isSlowDown(long instanceId) {
        /*
         * There's only one factor we need to consider currently:
         *
         * 0. too many finished instances in cache(Applier is too slow)
         */
        return true;
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
        return null;
    }

    private void startTasks() {
        // heartbeat task

        // detect task

        //
    }

    @Override
    public void boot() {

    }

    @Override
    public Node local() {
        return null;
    }

    private void checkValue(ByteBuffer value) {
        if (state != State.RUNNING) {
            throw new IllegalStateException(
                String.format("group(%d) is not running(%s)", id, state.name())
            );
        }

        Objects.requireNonNull(value, "value");
        if (value.remaining() == 0) {
            throw new IllegalArgumentException("can't commit empty value");
        }

        if (proposer == null) {
            throw new IllegalStateException(
                String.format("group(%d/%s) is unable to commit value", id, role.name())
            );
        }
    }

    public Commit commit(ByteBuffer value) {
        checkValue(value);
        return proposer.commit(value);
    }

    @Override
    public CommitResult commit(ByteBuffer value, Object att) {
        checkValue(value);
        return proposer.commit(value, att);
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
        return null;
    }

    @Override
    public void process(Message msg) {
        Objects.requireNonNull(msg, "msg");

        int dstId = msg.getGroupId();
        if (dstId != id) {
            throw new IllegalStateException(
                String.format("buggy, group(%d) can't process messages belong to group(%d)", id, dstId)
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

    private void dispatch(Message msg) {
        if (isConfig(msg.op())) {
            processConfig((Reconfigure)msg);
        } else if (isLearner(msg)) {

        }
    }

    private boolean isConfig(Opcode op) {
        return op == Opcode.JOIN_GROUP;
    }

    private boolean isLearner(Message msg) {
        return msg.op() == Opcode.LEARN_VALUE;
    }

    private void processLearner(Message msg) {
        if (msg.op() == Opcode.LEARN_VALUE) {
            tryLearn((LearnValue)msg);
        }
    }

    private void tryLearn(LearnValue msg) {
        PaxosInstance instance = cache.get(msg.getInstanceId());
        if (instance == null) {
            // this node didn't vote for the instance?
            return ;
        }

        if (instance.accepted().compare(msg.getSequence(), msg.getNodeId()).isEquals()) {

        }
    }

    private void learnValue(PaxosInstance instance) {
        if (hasLearned(instance)) {
            return ;
        }


    }

    private long checkpoint = 0;

    private List<PaxosInstance> chosen = new ArrayList<>();
    private List<PaxosInstance> batch = new ArrayList<>();

    private int getSequentialChosenInstances(List<PaxosInstance> chosen) {
        int count = 0;
        for (;;) {
            PaxosInstance instance = cache.get(checkpoint);
            if (instance == null) {
                return count;
            }

            if (instance.isChosen()) {
                chosen.add(instance);
                count++;
                continue;
            }

            break;
        }
        return count;
    }

    private void processChosenValues() {
        int count = getSequentialChosenInstances(chosen);
        if (count == 0) {
            return ;
        }

        for (int i = 0; i < count; i++) {
            PaxosInstance instance = chosen.get(i);
            if (instance.isConfig()) {
                if (batch.size() > 0) {
                    applyUserValues(batch);
                    batch.clear();
                }
                applyConfigValue(instance);
            } else {
                batch.add(instance);

                if (batch.size() >= 64) {
                    applyUserValues(batch);
                    batch.clear();
                }
            }
        }

        if (batch.size() > 0) {
            applyUserValues(batch);
            batch.clear();
        }

        chosen.clear();
    }

    private void applyConfigValue(PaxosInstance instance) {
        ByteBuf value = instance.chosenValue();
        if (value == null) {
            throw new IllegalArgumentException("no chosen value");
        }

        if (value.readableBytes() == 0) {
            // a empty value?
            return ;
        }

        // parse type
        ValueType type = ValueUtils.parse(value);
        if (type == null) {
            throw new IllegalStateException("can't parse the type of value");
        }

        if (type.isMemberJoin()) {
            addMember(instance);
        }
    }

    private void addMember(PaxosInstance instance) {

    }

    private void applyUserValues(List<PaxosInstance> batch) {

    }

    private boolean hasLearned(PaxosInstance instance) {
        return false;
    }

    private void processConfig(Reconfigure msg) {
        // checks whether or not this current is able to process this request
        if (role != Role.FORMAL) {
            throw new IllegalStateException(
                String.format("current in group(%d) is %s, it can't process reconfigure event(%s)", id, role.name(), msg.op().name())
            );
        }

        if (msg.op() == Opcode.JOIN_GROUP) {
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
                commit(buf.nioBuffer(), null);
            } finally {
                buf.release();
            }
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

        // disconnect connections connected with other servers

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
        if (acceptor != null) {
            close(acceptor, String.format("acceptor of group(%d)", id));
        }

        close(learner, String.format("leaner of group(%d)", id));
        close(applier, String.format("applier of group(%d)", id));
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
    public boolean isClosed() {
        return state == State.SHUTTING_DOWN || state == State.SHUTDOWN;
    }

    @Override
    public String toString() {
        return null;
    }

}
