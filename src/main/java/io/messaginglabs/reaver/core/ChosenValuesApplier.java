package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.dsl.CheckpointStateMachine;
import io.messaginglabs.reaver.dsl.ChosenValues;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.messaginglabs.reaver.dsl.ValueCtx;
import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChosenValuesApplier implements Applier {

    private static final Logger logger = LoggerFactory.getLogger(ChosenValuesApplier.class);

    private static final int ST_INIT = 0;
    private static final int ST_RUNNING = 1;
    private static final int ST_SHUTTING_DOWN = 2;
    private static final int ST_TERMINATED = 3;

    private Queue<PaxosInstance> instances;
    private StateMachine dst;

    private int groupId;
    private Itr itr;
    private Thread worker;
    private long checkpoint = 0;
    private long flushCheckpoint = 0;
    private volatile int state;

    private final String name;

    private Consumer<Throwable> consumer;

    public ChosenValuesApplier(StateMachine dst, int groupId, String name) {
        this.dst = Objects.requireNonNull(dst, "dst");
        this.groupId = groupId;
        this.state = ST_INIT;
        this.name = name;
    }

    private String convertState(int state) {
        if (state == ST_INIT) {
            return "INIT";
        } else if (state == ST_RUNNING) {
            return "RUNNING";
        } else if (state == ST_SHUTTING_DOWN) {
            return "CLOSING";
        } else if (state == ST_TERMINATED) {
            return "TERMINATED";
        } else {
            return "UNKNOWN";
        }
    }

    public void initCheckpoint() {
        if (dst instanceof CheckpointStateMachine) {
            CheckpointStateMachine sm = (CheckpointStateMachine)dst;
            checkpoint = flushCheckpoint = sm.getCheckpoint();
        }

        logger.info("init checkpoint({}) of applier({}) of group({})", checkpoint, name, groupId);
    }

    @Override
    public void start() throws Exception {
        if (state != ST_INIT) {
            throw new IllegalStateException(
                String.format("the applier is not in INIT state, it's %s", convertState(state))
            );
        }

        worker = new Thread(this::run, String.format("%s-%s-%d", Defines.UNIFIED_THREAD_NAME_PREFIX, this.name, groupId));
        worker.start();

        state = ST_RUNNING;
    }

    public void run() {
        initCheckpoint();

        while (state <= ST_RUNNING) {
            try {
                apply();
            } catch (Exception cause) {
                /*
                 * once we caught a exception, stop applying chosen values
                 * to the state machine and freeze the group
                 */
                if (logger.isErrorEnabled()) {
                    logger.error(
                        "applier({}) caught unknown exception, freeze the group({}), checkpoints(apply={}, flush={}), pending values({})",
                        name,
                        groupId,
                        checkpoint,
                        flushCheckpoint,
                        instances.size()
                    );
                }

                if (consumer != null) {
                    consumer.accept(cause);
                }

                state = ST_TERMINATED;
                return ;
            }
        }
    }

    @Override
    public void addExceptionListener(Consumer<Throwable> consumer) {
        this.consumer = consumer;
    }

    private void apply() {

    }

    @Override
    public void close() throws Exception {
        synchronized (this) {
            if (state >= ST_SHUTTING_DOWN) {
                throw new IllegalStateException(
                    String.format("buggy, the applier is in %s state", convertState(state))
                );
            }

            logger.info("starts to close applier({}.{}), ", name, groupId);

            state = ST_SHUTTING_DOWN;
            wakeupWorker();
            worker.join();

            int count = instances.size();
            if (count > 0) {
                throw new IllegalStateException(
                    String.format("buggy, there're %d instances in buffer", count)
                );
            }

            logger.info("applier({}.{}) is terminated, checkpoints(apply={}, flush={})", name, groupId, checkpoint, flushCheckpoint);
        }
    }

    private void wakeupWorker() {

    }

    @Override
    public void add(PaxosInstance instance) {
        Objects.requireNonNull(instance, "instance");

        if (instance.refCnt() == 0) {
            throw new IllegalArgumentException("released instance");
        }

        ByteBuf value = instance.chosen().getValue();
        if (value == null) {
            throw new IllegalArgumentException(
                String.format("no chosen myValue in instance(%d) of group(%d)", instance.id(), groupId)
            );
        }

        if (value.refCnt() == 0 || value.readableBytes() == 0) {
            throw new IllegalArgumentException(
                String.format(
                    "invalid value(%d/%d) in instance(%d) of group(%d)",
                    value.refCnt(),
                    value.readableBytes(),
                    instance.id(),
                    groupId
                )
            );
        }

        if (state >= ST_SHUTTING_DOWN) {
            throw new IllegalStateException(
                String.format("buggy, applier(%s.%d) is shutting down(%s)", name, groupId, convertState(state))
            );
        }

        if (!instances.offer(instance)) {
            throw new IllegalStateException(
                String.format("can't push new value to queue(%d)", instances.size())
            );
        }
    }

    public int doApply() {
        int count = 0;
        while (instances.size() > 0) {
            PaxosInstance instance = instances.poll();

            if (!doApply(instance)) {

            }

            count++;
        }

        return count;
    }

    public boolean doApply(PaxosInstance instance) {
        // converts the myValue to DSL
        return false;
    }

    final class Itr implements Iterator<ChosenValues> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public ChosenValues next() {
            return null;
        }
    }

    final class DefaultChosenValues implements ChosenValues {

        private long id;


        @Override
        public long instanceId() {
            return 0;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public ValueCtx next() {
            return null;
        }
    }
}
