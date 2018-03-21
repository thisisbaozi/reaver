package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.DefaultServerConnector;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.com.NettyTransporter;
import io.messaginglabs.reaver.com.ServerConnector;
import io.messaginglabs.reaver.com.Transporter;
import io.messaginglabs.reaver.dsl.ElectionPolicy;
import io.messaginglabs.reaver.dsl.Group;
import io.messaginglabs.reaver.dsl.GroupsBuilder;
import io.messaginglabs.reaver.dsl.LeaderSelector;
import io.messaginglabs.reaver.log.DefaultLogStorage;
import io.messaginglabs.reaver.log.LogStorage;
import io.messaginglabs.reaver.utils.Parameters;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.PlatformDependent;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaxosGroupsBuilder implements GroupsBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PaxosGroupsBuilder.class);

    private final ConcurrentMap<Integer, PaxosGroup> groupsCache = new ConcurrentHashMap<>();

    // basic
    private String prefix = "default";
    private boolean debug = false;
    private GroupOptions options = new GroupOptions();

    // com
    private int port = 9666;
    private int ioThreads = 4;
    private String localAddress = null;

    // execute
    private int executors = 0;
    private boolean exclusiveExecutor = false;

    // storage
    private String path;

    // resource
    private boolean initialized = false;
    private ByteBufAllocator allocator;
    private Transporter transporter;
    private LogStorage storage;
    private EventExecutorGroup executorsGroup;
    private ServerConnector connector;

    @Override
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void setIoThreads(int num) {
        this.ioThreads = num;
    }

    @Override
    public void setExecutorThreads(int num) {
        this.executors = num;
    }

    @Override
    public void setUseExclusiveAlgorithmExecutor(boolean enable) {
        this.exclusiveExecutor = enable;
    }

    @Override
    public void setEnableStatistics(boolean enable) {
        options.statistics = enable;
    }

    @Override
    public void setBatchSize(int maxBatchSize) {
        options.batch = maxBatchSize;
    }

    @Override
    public void setPipeline(int pipeline) {
        options.pipeline = pipeline;
    }

    @Override
    public void setValueCacheCapacity(int capacity) {
        options.valueCacheCapacity = capacity;
    }

    @Override
    public void setDir(String path) {
        this.path = path;
    }

    @Override
    public void setElectionPolicy(ElectionPolicy policy) {
        options.policy = policy;
    }

    @Override
    public void setLeaderSelector(LeaderSelector selector) {
        options.selector = selector;
    }

    @Override
    public void setLeaderProposeOnly(boolean enable) {
        options.leaderProposeOnly = enable;
    }

    @Override
    public void setLeaseDuration(int duration) {
        options.leaseDuration = duration;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void setLocalAddress(String address) {
        this.localAddress = address;
    }

    @Override
    public Group build(int id) throws Exception {
        return build(id, this.debug);
    }

    @Override
    public Group build(int id, boolean debug) throws Exception {
        return build(id, debug, this.path);
    }

    @Override
    public Group build(int id, boolean debug, String path) throws Exception {
        return build(id, debug, path, exclusiveExecutor, prefix);
    }

    @Override
    public Group build(int id, boolean debug, String path, boolean exclusive, String prefix) throws Exception {
        synchronized (this) {
            if (!initialized) {
                init();
            }

            if (groupsCache.containsKey(id)) {
                throw new IllegalStateException(
                    String.format("group(%d) is already exist.", id)
                );
            }

            // Creates a group
            GroupEnv env = initEnv(debug, exclusiveExecutor, path);

            // Do not GroupState the given id is duplicated or not.
            PaxosGroup newGroup = new MultiPaxosGroup(id, env, options);
            groupsCache.put(id, newGroup);

            return newGroup;
        }
    }

    private GroupEnv initEnv(boolean debug, boolean exclusive, String path) throws Exception {
        GroupEnv env = new GroupEnv();
        if (!path.equals(this.path)) {
            /*
             * Creates a new one.
             */
            LogStorage storage = new DefaultLogStorage(path);
            storage.init();

            env = new GroupEnv();
            env.storage = storage;
        } else {
            env.storage = this.storage;
        }

        env.allocator = allocator;
        env.transporter = transporter;
        env.debug = debug;
        env.connector = connector;

        if (exclusive) {
            env.executor = new DefaultEventExecutor(new DefaultThreadFactory(prefix + "-core"));
            env.applier = new DefaultEventExecutor(new DefaultThreadFactory(prefix + "-applier"));
        } else {
            if (executorsGroup == null) {
                executorsGroup = new DefaultEventExecutorGroup(executors, new DefaultThreadFactory(prefix + "-core"));
            }

            EventExecutor executor = executorsGroup.next();
            env.executor = executor;
            env.applier = executor;
        }

        return env;
    }

    private void init() throws Exception {
        // GroupState
        Parameters.requireNotEmpty(localAddress, "localAddress");
        Parameters.requireNotEmpty(path, "path");

        if (port <= 0) {
            throw new IllegalArgumentException("invalid port");
        }
        if (ioThreads <= 0) {
            throw new IllegalArgumentException("the number of io threads must be greater than 0, but given: " + ioThreads);
        }
        if (!exclusiveExecutor && executors <= 0) {
            throw new IllegalArgumentException("the number of executors  must be greater than 0, but given: " + executors);
        }

        // com
        transporter = new NettyTransporter(localAddress, port, ioThreads, prefix);
        transporter.setConsumer(this::dispatch);
        transporter.init();

        // storage
        storage = new DefaultLogStorage(path);
        storage.init();

        // executor
        if (!exclusiveExecutor) {
            if (executors <= 0) {
                throw new IllegalArgumentException("the number of executor threads must be greater than 0, but given: " + executors);
            }

            ThreadFactory factory = new DefaultThreadFactory(prefix);
            executorsGroup = new DefaultEventExecutorGroup(executors, factory);
        }

        // buffer
        if (!PlatformDependent.hasUnsafe()) {
            logger.warn("Unsafe is not supported, this could affect throughput deeply");
        }

        /*
         * init buffer allocator
         */
        boolean useDirect = PlatformDependent.directBufferPreferred();
        if (!useDirect) {
            logger.warn("can't use direct buffer, this could affect throughput");
        }

        if (debug) {
            /*
             * tracking memory in advanced way for avoiding memory leaking
             */
            System.setProperty("io.netty.leakDetection.level", ResourceLeakDetector.Level.ADVANCED.name());
        }
        allocator = new PooledByteBufAllocator(useDirect);
        connector = new DefaultServerConnector(1024);

        initialized = true;
    }

    public void dispatch(Message msg) {
        Objects.requireNonNull(msg, "msg");

        PaxosGroup group = groupsCache.get(msg.getGroupId());
        if (group == null) {
            /*
             * todo: throws a exception or just logging ?
             */
            return ;
        }

        try {
            group.process(msg);
        } catch (Exception cause) {
            if (group.isClosed()) {
                logger.info("ignore message({}), the group({}) is closed", msg.toString(), group.id());
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("group({}) can't process message({})", group.toString(), msg.toString());
                }
            }
        }
    }


}
