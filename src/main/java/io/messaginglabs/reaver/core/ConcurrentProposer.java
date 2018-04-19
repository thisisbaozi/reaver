package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.config.GroupConfigs;
import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.utils.Parameters;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrentProposer extends AlgorithmParticipant implements Proposer {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentProposer.class);

    private final int groupId;
    private final long nodeId;

    // options
    private int parallel = 10;
    private int valueCacheCapacity = 1024;
    private int maxBatchSize = 1024 * 1024 * 128;   // 128m
    private int timeout = 3000; // 3 seconds
    private int instanceCacheCapacity = 1024 * 4;
    private int recycleInstancesSize = 32;
    private boolean acceptDirectly = true;

    // components
    private final Sequencer sequencer;
    private final GroupConfigs configs;
    private InstanceCache cache;
    private ByteBufAllocator allocator;

    private Queue<GenericCommit> values;
    private SerialProposer[] proposers;
    private GenericCommit reserved = null;

    private final ScheduledExecutorService executor;
    private final Runnable proposeTask = this::propose;

    public ConcurrentProposer(int groupId, long localId, GroupConfigs configs, ScheduledExecutorService executor) {
        this.groupId = groupId;
        this.nodeId = localId;

        this.configs = configs;
        this.executor = executor;
        this.allocator = ByteBufAllocator.DEFAULT;

        this.sequencer = new IdSequencer();
    }

    public void setParallel(int parallel) {
        this.parallel = Parameters.requireNotNegativeOrZero(parallel, "parallel");
    }

    public void setValueCacheCapacity(int valueCacheCapacity) {
        this.valueCacheCapacity = Parameters.requireNotNegativeOrZero(valueCacheCapacity, "valueCacheCapacity");
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = Parameters.requireNotNegativeOrZero(maxBatchSize, "maxBatchSize");
    }

    public void setTimeout(int timeout) {
        this.timeout = Parameters.requireNotNegativeOrZero(timeout, "timeout");
    }

    public void setInstanceCacheCapacity(int instanceCacheCapacity) {
        this.instanceCacheCapacity = Parameters.requireNotNegativeOrZero(instanceCacheCapacity, "instanceCacheCapacity");
    }

    public void setRecycleInstancesSize(int recycleInstancesSize) {
        this.recycleInstancesSize = Parameters.requireNotNegativeOrZero(recycleInstancesSize, "recycleInstancesSize");
    }

    public void setAllocator(ByteBufAllocator allocator) {
        this.allocator = Objects.requireNonNull(allocator, "allocator");
    }

    public void setAcceptDirectly(boolean acceptDirectly) {
        this.acceptDirectly = acceptDirectly;

        if (this.proposers != null) {
            for (SerialProposer proposer : proposers) {
                proposer.setAcceptDirectly(acceptDirectly);
            }
        }
    }

    @Override
    public void init() throws Exception {
        /*
         * a multi producer single consumer queue is a better choice.
         */
        this.values = new ArrayBlockingQueue<>(valueCacheCapacity);
        this.proposers = new SerialProposer[parallel];
        this.cache = new DefaultInstanceCache(instanceCacheCapacity, recycleInstancesSize);

        Consumer<SerialProposer> consumer = proposer -> {
            /*
             * invoked once a proposer is ready to propose a new value.
             */
            proposeRightNow();
        };

        for (int i = 0; i < parallel; i++) {
            this.proposers[i] = new DefaultSerialProposer(groupId, i, nodeId, cache, sequencer, configs, allocator, executor);
            this.proposers[i].setTimeout(timeout);
            this.proposers[i].observe(SerialProposer.State.FREE, consumer);
            this.proposers[i].setAcceptDirectly(acceptDirectly);
            this.proposers[i].init();
        }
    }

    @Override
    public Sequencer sequencer() {
        return sequencer;
    }

    @Override
    public InstanceCache cache() {
        return cache;
    }

    @Override
    public CommitResult commit(ByteBuf value, Object attachment) {
        GenericCommit commit = newCommit(value, attachment);
        return propose(commit) ? CommitResult.OK : CommitResult.PROPOSE_THROTTLE;
    }

    @Override
    public Commit commit(ByteBuf value) {
        GenericCommit commit = newCommit(value, null);

        if (!propose(commit)) {
            commit.setFailure(CommitResult.PROPOSE_THROTTLE);
        }

        return commit;
    }

    private boolean propose(GenericCommit commit) {
        if (!enqueue(commit)) {
            return false;
        }

        if (!proposeRightNow()) {
            logger.warn("can't execute proposing task in group({}), executor({})", groupId);
        }

        return true;
    }

    public int getCommits() {
        return values.size() + (reserved != null ? 1 : 0);
    }

    public boolean enqueue(GenericCommit commit) {
        boolean result = values.offer(commit);
        if (!result) {
            /*
             * buffer is full, pop the head element of the queue and append
             * the last one.
             */
            int times = 6;
            if (times > valueCacheCapacity) {
                times = valueCacheCapacity;
            }

            for (;;) {
                result = values.offer(commit);
                if (result) {
                    break;
                }

                GenericCommit predecessor = values.poll();
                if (predecessor == null) {
                    continue;
                }
                predecessor.setFailure(CommitResult.PROPOSE_THROTTLE);

                times--;
                if (times == 0) {
                    break;
                }
            }
        }

        return result;
    }

    private boolean proposeRightNow() {
        if (executor == null) {
            proposeTask.run();
            return true;
        }

        try {
            executor.execute(proposeTask);
        } catch (RejectedExecutionException cause) {
            /*
             * too many tasks in this executor
             */
            if (logger.isDebugEnabled()) {
                logger.error("can't execute propose task due to too many tasks in executor({})", groupId);
            }

            return false;
        }

        return true;
    }

    public void propose() {
        inLoop();

        while (values.size() > 0) {
            if (proposeOneValue() == null) {
                break;
            }
        }
    }

    public SerialProposer proposeOneValue() {
        assert (values.size() > 0);

        SerialProposer proposer = find();
        if (proposer == null) {
            /*
             * can't match a free proposer, try again later
             */
            if (logger.isDebugEnabled()) {
                logger.warn("can't match a free proposer from group({}), proposers({})", groupId, dumpProposersState());
            }

            // do statistics

            /*
             * we don't know when there's a free proposer, proposers should
             * propose once they are free.
             */
            return null;
        }

        propose(proposer);
        return proposer;
    }

    public int batch(List<GenericCommit> batch) {
        int bytes = 0;
        while (values.size() > 0) {
            GenericCommit commit;
            if (reserved != null) {
                commit = reserved;
                reserved = null;
            } else {
                commit = values.poll();
            }

            if (isIgnore(commit)) {
                continue;
            }

            if (commit.stage() != Commit.Stage.PROPOSED && !commit.setStage(Commit.Stage.PROPOSED)) {
                if (!commit.isCancelled()) {
                    throw new IllegalStateException(String.format("learn(%s) is not cancelled", commit.toString()));
                }

                continue;
            }

            if (!inBatch(commit) || commit.valueSize() + bytes >= maxBatchSize) {
                if (batch.isEmpty()) {
                    batch.add(commit);
                } else {
                    /*
                     * reserve it for proposing in next round
                     */
                    assert (reserved == null);
                    reserved = commit;
                }

                break;
            }

            batch.add(commit);
            bytes += commit.valueSize();
        }

        return bytes;
    }

    private void propose(SerialProposer proposer) {
        Objects.requireNonNull(proposer, "proposer");

        if (proposer.isBusy()) {
            throw new IllegalStateException(
                String.format("buggy, proposer(%s) of group(%s) is not free", proposer.toString(), groupId)
            );
        }

        if (values.isEmpty()) {
            throw new IllegalStateException("no commits in buffer");
        }

        List<GenericCommit> buffer = proposer.valueCache();
        int bytes = batch(buffer);
        int count = buffer.size();

        if (count > 0 && bytes > 0) {
            proposer.commit(buffer);
        }
    }

    private boolean inBatch(GenericCommit commit) {
        /*
         * There's only ChosenValue getOp can be proposed in batch.
         */
        return commit.valueType().isAppData();
    }

    private boolean isIgnore(GenericCommit commit) {
        if (commit.isCancelled()) {
            /*
             * ignore it if a learn has been cancelled
             */
            logger.debug("a learn({}) posted to group({}) is cancelled, ignore it", commit.toString(), groupId);
            return true;
        }

        if (commit.isDone()) {
            logger.debug("a learn({}) posted to group({}) is done, ignore it", commit.toString(), groupId);
            return true;
        }

        return false;
    }

    private String dumpProposersState() {
        return null;
    }

    public SerialProposer find() {
        for (SerialProposer proposer : proposers) {
            if (!proposer.isBusy()) {
                return proposer;
            }
        }
        return null;
    }

    public GenericCommit newCommit(ByteBuf value, Object attachment) {
        Objects.requireNonNull(value, "value");

        if (value.refCnt() == 0) {
            throw new IllegalArgumentException("released value");
        }
        if (value.readableBytes() == 0) {
            throw new IllegalArgumentException("empty value is not allowed");
        }

        return new GenericCommit(value, attachment);
    }

    public void process(Message msg) {
        Objects.requireNonNull(msg, "msg");

        AcceptorReply reply = (AcceptorReply)msg;

        PaxosInstance instance = cache.get(reply.getInstanceId());
        if (instance == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "can't find instance({}) in group({}), ignores reply",
                    reply.getInstanceId(),
                    msg.getGroupId()
                );
            }

            return ;
        }

        int proposerId = instance.holder();
        if (proposerId == -1) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "no proposer holds the instance({}), ignores reply",
                    reply.getInstanceId()
                );
            }

            return ;
        }

        if (proposerId < 0 || proposerId >= proposers.length) {
            throw new IllegalStateException(String.format("no proposer(%d)", proposerId));
        }

        proposers[proposerId].process(reply);
    }

    @Override
    public boolean close(long timeout) {
        return false;
    }
}
