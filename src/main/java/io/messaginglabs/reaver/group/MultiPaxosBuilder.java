package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.DefaultServerConnector;
import io.messaginglabs.reaver.com.NettyTransporter;
import io.messaginglabs.reaver.com.ServerConnector;
import io.messaginglabs.reaver.com.Transporter;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.dsl.ElectionPolicy;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.dsl.PaxosBuilder;
import io.messaginglabs.reaver.dsl.LeaderSelector;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.messaginglabs.reaver.log.DefaultLogStorage;
import io.messaginglabs.reaver.log.LogStorage;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiPaxosBuilder implements PaxosBuilder {

    private static final Logger logger = LoggerFactory.getLogger(MultiPaxosBuilder.class);
    private static final String ADDRESS_RESOLVED_BY_INTERFACE;

    static {
        InetAddress address = null;
        SocketException cause = null;
        try {
            address = AddressUtils.resolveIpV4();
        } catch (SocketException e) {
            cause = e;
        }

        if (address == null) {
            logger.warn("can't parse local address by interfaces due to {}", cause != null ? cause.getMessage() : "unknown reason");
            ADDRESS_RESOLVED_BY_INTERFACE = null;
        } else {
            ADDRESS_RESOLVED_BY_INTERFACE = address.getHostAddress();
            logger.info("resolved local address({})", ADDRESS_RESOLVED_BY_INTERFACE);
        }
    }

    // basic
    private int id;
    private String prefix = "default";
    private boolean debug = false;
    private GroupOptions options = new GroupOptions();

    // com
    private Node node = new Node();

    // storage
    private String path;

    // resource
    private ByteBufAllocator allocator;

    private boolean externalTransporter = false;
    private Transporter transporter;

    private boolean externalStorage = false;
    private LogStorage storage;
    private ServerConnector connector;

    public MultiPaxosBuilder(int id, String name) {
        this.id = id;
        this.prefix = name;
        this.node.setIp(ADDRESS_RESOLVED_BY_INTERFACE);
        this.node.setPort(8655);
    }

    @Override
    public void setLogStorage(LogStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.externalStorage = true;
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
    public void setNode(Node node) {
        this.node = Objects.requireNonNull(node, "node");
    }

    @Override
    public void setTransporter(Transporter transporter) {
        this.transporter = Objects.requireNonNull(transporter, "transporter");
        this.externalTransporter = true;
    }

    @Override
    public PaxosGroup build(StateMachine stateMachine) throws Exception {
        Objects.requireNonNull(stateMachine, "stateMachine");

        synchronized (this) {
            init();

            InternalPaxosGroup group = new MultiPaxosGroup(id, stateMachine, initEnv(debug, path), options);
            // transporter.setConsumer(group::process);

            /*
             * releases resources if the group is closed
             */
            group.addCloseListener(() -> close(group));
            return group;
        }
    }

    private void close(InternalPaxosGroup group) {
        if (!externalTransporter) {
            try {
                transporter.close();
            } catch (Exception cause) {
                logger.warn("can't close transporter", cause);
            }
        }

        if (!externalStorage) {
            try {
                storage.close();
            } catch (IOException cause) {
                logger.warn("can't close storage", cause);
            }
        }

        group.env().executor.shutdown();
        group.env().applier.shutdown();
    }

    private GroupEnv initEnv(boolean debug, String path) throws Exception {
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

        env.debug = debug;
        env.allocator = allocator;
        env.transporter = transporter;
        env.connector = connector;
        env.executor = new DefaultEventExecutor(new DefaultThreadFactory(prefix + "-core"));
        env.applier = new DefaultEventExecutor(new DefaultThreadFactory(prefix + "-applier"));

        return env;
    }

    private void init() throws Exception {
        // com
        if (transporter != null) {
            transporter = new NettyTransporter(node.getIp(), node.getPort(), 1, prefix);
            transporter.init();
        }

        // storage
        if (storage == null) {
            storage = new DefaultLogStorage(path);
            storage.init();
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
        connector = new DefaultServerConnector(-1, debug, transporter);
    }

}
