package io.messaginglabs.reaver.core;

import com.sun.xml.internal.ws.addressing.v200408.MemberSubmissionWsaServerTube;
import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import io.messaginglabs.reaver.group.MultiPaxosGroup;
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

public class ParallelProposer extends AlgorithmParticipant implements Proposer {

    private static final Logger logger = LoggerFactory.getLogger(ParallelProposer.class);

    private final Queue<GenericCommit> values;
    private final int cacheCapacity;
    private final int maxBatchSize;

    private final SerialProposer[] proposers;
    private GenericCommit reserved = null;

    /*
     * for reducing memory footprint
     */
    private final Runnable proposeTask = this::propose;

    public ParallelProposer(int cacheCapacity, int maxBatchSize, int parallel, InternalPaxosGroup group) {
        super(group);
        if (cacheCapacity <= 0) {
            throw new IllegalArgumentException("myValue cache capacity must greater than 0, but given " + cacheCapacity);
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
            this.proposers[i] = new DefaultSerialProposer(i, group);
            this.proposers[i].observe(SerialProposer.State.FREE, consumer);
        }
    }

    @Override
    public CommitResult commit(ByteBuf value, Object attachment) {
        if (group().state() == PaxosGroup.State.FROZEN) {
            return CommitResult.FROZEN_GROUP;
        }

        // wrap myValue and attachment with a learn
        GenericCommit commit = newCommit(value, attachment);
        return enqueue(commit) ? CommitResult.OK : CommitResult.PROPOSE_THROTTLE;
    }

    @Override
    public Commit commit(ByteBuf value) {
        GenericCommit commit = newCommit(value, null);

        if (group().state() == PaxosGroup.State.FROZEN) {
            commit.setFailure(CommitResult.FROZEN_GROUP);
            return commit;
        }

        if (!enqueue(commit)) {
            commit.setFailure(CommitResult.PROPOSE_THROTTLE);
        }

        return commit;
    }

    private boolean enqueue(GenericCommit commit) {
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

        if (result) {
            if (!proposeRightNow()) {
                logger.warn("can't execute proposing task in group({}), executor({})", group().id());
            }
        }

        return result;
    }

    private boolean proposeRightNow() {
        ExecutorService executor = group().env().executor;

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

    private void propose() {
        inLoop();

        while (!values.isEmpty()) {
            SerialProposer proposer = find();
            if (proposer == null) {
                /*
                 * can't match a free proposer, try again later
                 */
                if (logger.isDebugEnabled()) {
                    logger.warn("can't match a free proposer from group({}), proposers({})", group().id(), dumpProposersState());
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
        Objects.requireNonNull(proposer, "proposer");

        if (proposer.isBusy()) {
            throw new IllegalStateException(
                String.format("buggy, proposer(%s) of group(%s) is not free", proposer.toString(), group().id())
            );
        }

        if (values.isEmpty()) {
            throw new IllegalStateException("no commits in buffer");
        }

        int bytes = 0;
        List<GenericCommit> batch = proposer.valueCache();
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

            if (!commit.setStage(Commit.Stage.PROPOSED)) {
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

        if (!batch.isEmpty()) {
            proposer.commit(batch);
        }
    }

    private boolean inBatch(GenericCommit commit) {
        /*
         * There's only ValueCtx op can be proposed in batch.
         */
        return commit.valueType().isAppData();
    }

    private boolean isIgnore(GenericCommit commit) {
        if (commit.isCancelled()) {
            /*
             * ignore it if a learn has been cancelled
             */
            logger.debug("a learn({}) posted to group({}) is cancelled, ignore it", commit.toString(), group().id());
            return true;
        }

        if (commit.isDone()) {
            logger.debug("a learn({}) posted to group({}) is done, ignore it", commit.toString(), group().id());
            return true;
        }

        return false;
    }

    private String dumpProposersState() {
        return null;
    }

    private SerialProposer find() {
        for (SerialProposer proposer : proposers) {
            if (!proposer.isBusy()) {
                return proposer;
            }
        }
        return null;
    }

    private GenericCommit newCommit(ByteBuf value, Object attachment) {
        Objects.requireNonNull(value, "myValue");

        if (value.readableBytes() == 0) {
            throw new IllegalArgumentException("empty myValue is not allowed");
        }

        return new GenericCommit(value, attachment);
    }

    public void process(Message msg) {
        Objects.requireNonNull(msg, "msg");

        AcceptorReply reply = (AcceptorReply)msg;
        int proposerId = reply.getProposerId();
        SerialProposer proposer = proposers[proposerId];
        proposer.process(reply);
    }


    @Override
    public boolean close(long timeout) {
        return false;
    }

    @Override
    public InternalPaxosGroup group() {
        return group;
    }
}
