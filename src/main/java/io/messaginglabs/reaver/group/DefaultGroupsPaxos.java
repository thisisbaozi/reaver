package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.NettyTransporter;
import io.messaginglabs.reaver.com.ServerConnector;
import io.messaginglabs.reaver.com.Transporter;
import io.messaginglabs.reaver.dsl.ElectionPolicy;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.dsl.PaxosOptions;
import io.messaginglabs.reaver.dsl.ThreadingModel;
import io.messaginglabs.reaver.log.LogStorage;
import io.messaginglabs.reaver.utils.FileSystemUtils;
import io.messaginglabs.reaver.utils.Parameters;
import io.netty.util.concurrent.EventExecutorGroup;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

public final class DefaultGroupsPaxos implements GroupsPaxos {

    private boolean isClosed = false;
    private ConcurrentMap<Integer, PaxosGroup> groups;

    private PaxosOptions options;
    private ServerConnector connector;
    private LogStorage storage;
    private Transporter transporter;
    private EventExecutorGroup executors;

    @Override
    public void init(PaxosOptions options) throws Exception {
        this.options = validate(options);

        /*
         * initializes following:
         *
         * 0. RPC
         * 1. storage
         * 2. threads pool(if it's necessary)
         * 3. buf allocator
         */
        initCom();
    }

    private PaxosOptions validate(PaxosOptions options) throws IOException {
        Objects.requireNonNull(options, "options");

        // performance
        Parameters.requireNotNegativeOrZero(options.batch, "batch");
        Parameters.requireNotNegativeOrZero(options.pipeline, "pipeline");
        Parameters.requireNotNegativeOrZero(options.valuesCache, "valuesCache");

        // com
        Objects.requireNonNull(options.node, "current");
        Parameters.requireNotNegativeOrZero(options.node.getPort(), "current.port");
        Parameters.requireNotEmpty(options.node.getIp(), "current.ip");

        // storage
        if (options.storage == null) {
            File file = FileSystemUtils.mkdirsIfNecessary(options.path);
            FileSystemUtils.hasRWPermission(file);
        }

        // threading model
        Objects.requireNonNull(options.model, "model");
        if (options.model == ThreadingModel.THREADS_POOl) {
            Parameters.requireNotNegativeOrZero(options.executorPoolSize, "executorPoolSize");
        }

        // leader
        if (options.leaderProposeOnly) {
            Objects.requireNonNull(options.policy, "policy");
            if (options.policy == ElectionPolicy.CUSTOMIZED) {
                Objects.requireNonNull(options.selector, "selector");
            }

            Parameters.requireNotNegativeOrZero(options.leaseDuration , "leaseDuration");
        }

        return options;
    }

    private void initCom() {

    }

    private void initStorage() {

    }

    private void initBufAllocator() {

    }

    @Override
    public int size() {
        return groups.size();
    }

    @Override
    public PaxosGroup get(int id) {
        return groups.get(id);
    }

    @Override
    public PaxosGroup create(int id, boolean debug) {
        synchronized (this) {
            PaxosGroup group = get(id);
            if (group != null) {
                return group;
            }

            groups.put(id, doCreate(id, debug));
            return groups.get(id);
        }
    }

    private PaxosGroup doCreate(int id, boolean debug) {
        return null;
    }

    @Override
    public void close() throws Exception {
        synchronized (this) {
            if (isClosed) {
                throw new IllegalStateException("duplicated close");
            }
            isClosed = true;

            /*
             * two stages:
             *
             * 0. close active groups
             * 1. close resource(executors, storage, RPC)
             */
            // groups.values().forEach(PaxosGroup::close);


        }
    }
}
