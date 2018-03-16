package io.messaginglabs.jpaxos.core;

import io.messaginglabs.jpaxos.dsl.Commit;
import io.messaginglabs.jpaxos.dsl.CommitResult;
import io.messaginglabs.jpaxos.group.MultiPaxosGroup;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelProposer extends AbstractVoter implements Proposer {

    private static final Logger logger = LoggerFactory.getLogger(ParallelProposer.class);

    private final Queue<ValueCommit> values;
    private final int cacheCapacity;
    private final int maxBatchSize;

    private final SerialProposer[] proposers;

    public ParallelProposer(int cacheCapacity, int maxBatchSize, int parallel) {
        if (cacheCapacity <= 0) {
            throw new IllegalArgumentException("value cache capacity must greater than 0, but given " + cacheCapacity);
        }

        if (maxBatchSize <= 1024) {
            throw new IllegalArgumentException("batch size is too small, it must be greater than 1024");
        }

        if (parallel <= 0) {
            throw new IllegalArgumentException("the number of proposers must be greater than 0");
        }

        /*
         * a multi producer single consumer queue is a better choice.
         */
        this.values = new ArrayBlockingQueue<>(cacheCapacity);
        this.cacheCapacity = cacheCapacity;
        this.maxBatchSize = maxBatchSize;
        this.proposers = new SerialProposer[parallel];

        Consumer<SerialProposer> consumer = proposer -> {
            /*
             * for avoiding some proposers are hungry
             */
            proposeRightNow();
        };
        for (int i = 0; i < parallel; i++) {
            this.proposers[i] = new DefaultSerialProposer(i, group().env());
            this.proposers[i].observe(SerialProposer.State.FREE, consumer);
        }
    }

    @Override
    public Commit commit(ByteBuffer value) {
        ValueCommit commit = newCommit(value);

        boolean result = values.offer(commit);
        if (!result) {
            /*
             * buffer is full, pop the head element of the queue and append
             * the last one.
             */
            int times = 6;
            if (times > cacheCapacity) {
                times = cacheCapacity;
            }

            for (;;) {
                result = values.offer(commit);
                if (result) {
                    break;
                }

                ValueCommit predecessor = values.poll();
                if (predecessor == null) {
                    continue;
                }
                predecessor.setFailure(CommitResult.PROPOSE_THROTTLE);

                times--;
                if (times == 0) {
                    break;
                }
            }

            if (!result) {
                commit.setFailure(CommitResult.PROPOSE_THROTTLE);
                return commit;
            }
        }

        if (!proposeRightNow()) {
            commit.setFailure(CommitResult.PROPOSE_THROTTLE);
        }

        return commit;
    }

    private boolean proposeRightNow() {
        ExecutorService executor = executor();
        try {
            executor.execute(proposeTask);
        } catch (RejectedExecutionException cause) {
            /*
             * too many tasks in this executor
             */
            if (logger.isDebugEnabled()) {
                logger.error("can't execute propose task due to too many tasks in executor({})", group().id());
            }

            return false;
        }

        return true;
    }

    /*
     * for reducing memory footprint
     */
    private final Runnable proposeTask = this::propose;

    private void propose() {
        for (;;) {
            if (values.isEmpty()) {
                return;
            }

            SerialProposer proposer = find();
            if (proposer == null) {
                /*
                 * can't find a free proposer, try again later
                 */
                if (logger.isDebugEnabled()) {
                    logger.warn("can't find a free proposer from group({}), proposers({})", group().id(), dumpProposersState());
                }

                // do statistics

                /*
                 * we don't know when there's a free proposer, proposers should
                 * propose once they are free.
                 */
                return ;
            }

            process(proposer);
        }
    }

    private void process(SerialProposer proposer) {
        if (proposer.state() != SerialProposer.State.FREE) {
            throw new IllegalStateException(
                String.format("buggy, proposer(%s) of group(%s) is not free", proposer.toString(), group().id())
            );
        }

        // batch and propose
        int bytes = 0;
        List<ValueCommit> newBatch = proposer.newBatch();
        for (;;) {
            ValueCommit commit = values.poll();
            if (commit.isCancelled()) {
                /*
                 * ignore it if a commit has been cancelled
                 */
                logger.info("a commit({}) posted to group({}) is cancelled, ignore it", commit.toString(), group().id());
                continue;
            }

            if (commit.isDone()) {
                logger.info("a commit({}) posted to group({}) is done, ignore it", commit.toString(), group().id());
                continue;
            }

            /*
             * now, cancelling is not supported
             */
            commit.markNotCancellable();
            newBatch.add(commit);

            bytes += commit.getValueSize();
            if (bytes >= maxBatchSize) {
                break;
            }
        }

        if (!newBatch.isEmpty()) {
            proposer.commit(newBatch);
        }
    }

    private String dumpProposersState() {
        return null;
    }

    private SerialProposer find() {
        for (SerialProposer proposer : proposers) {
            if (proposer.state() == SerialProposer.State.FREE) {
                return proposer;
            }
        }
        return null;
    }

    private ByteBuf copyValue(ByteBuffer src) {
        ByteBufAllocator allocator = group().env().allocator;
        ByteBuf buf = allocator.buffer(src.remaining());
        buf.writeBytes(src);
        return buf;
    }

    private ValueCommit newCommit(ByteBuffer value) {
        Objects.requireNonNull(value, "value");

        if (!value.hasRemaining()) {
            throw new IllegalArgumentException("empty value is not allowed");
        }

        return new ValueCommit(executor(), copyValue(value));
    }

    @Override
    public ExecutorService executor() {
        return null;
    }

    @Override
    public MultiPaxosGroup group() {
        return null;
    }
}
