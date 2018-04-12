package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class GenericCommit extends ArrayList<GenericCommit.Pair> implements Commit {

    private static final long serialVersionUID = -1207413836374352596L;
    private static final Object CANCELLED_HOLDER = new Object();

    private final ByteBuf value;
    private final Object attachment;
    private final CommitType type;

    private int waiters;
    private long begin;
    private long done;
    private long instanceId;
    private Object result;
    private Stage stage;

    final class Pair {
        final Stage stage;
        final Consumer<Commit> consumer;

        Pair(Stage stage, Consumer<Commit> consumer) {
            this.stage = stage;
            this.consumer = consumer;
        }
    }

    public GenericCommit(ByteBuf value, Object attachment, CommitType type) {
        this.value = value;
        this.attachment = attachment;
        this.type = type;
        this.waiters = 0;
        this.begin = System.currentTimeMillis();
        this.done = -1;
        this.instanceId = Defines.VOID_INSTANCE_ID;
        this.stage = Stage.READY;
    }

    @Override
    public Stage stage() {
        return stage;
    }

    public CommitType type() {
        return type;
    }

    public int valueSize() {
        return value.readableBytes();
    }

    public ByteBuf value() {
        return value;
    }

    @Override
    public long instanceId() {
        return instanceId;
    }

    public boolean setStage(Stage stage) {
        Objects.requireNonNull(stage, "stage");

        synchronized (this) {
            if (isCancelled()) {
                return false;
            }

            if (stage.ordinal() <= this.stage.ordinal()) {
                throw new IllegalArgumentException(
                    String.format("current stage(%s), given stage(%s)", this.stage.name(), stage.name())
                );
            }

            this.stage = stage;
            if (this.stage == Stage.FINISHED) {
                this.result = CommitResult.OK;
                this.notifyWaiters();
            }

            notifyConsumers();

            return true;
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        /*
         * for influencing the correctness of the Paxos, we do not interrupt
         * the action this future associated with.
         */
        synchronized (this) {
            if (isCancelled()) {
                return true;
            }

            if (stage.ordinal() >= Stage.PROPOSED.ordinal()) {
                return false;
            }

            notifyWaiters();
            notifyConsumers();

            return true;
        }
    }

    @Override
    public boolean isCancelled() {
        return this.result == CANCELLED_HOLDER;
    }

    @Override
    public boolean isDone() {
        Object result = this.result;
        return result != null && result != CANCELLED_HOLDER;
    }

    @Override
    public CommitResult get() throws InterruptedException, ExecutionException {
        if (!isDone()) {
            checkInterrupted();

            synchronized (this) {
                while (!isDone()) {
                    waiters++;
                    try {
                        wait();
                    } finally {
                        waiters--;
                    }
                }
            }
        }

        if (result == CANCELLED_HOLDER) {
            throw new CancellationException(toString());
        }

        return result();
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    @Override
    public CommitResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!isDone()) {
            long nano = unit.toNanos(timeout);
            if (nano <= 0) {
                return result();
            }

            checkInterrupted();
            synchronized (this) {
                if (isDone()) {
                    return result();
                }

                waiters++;
                try {
                    wait(timeout / 1000000, (int)(nano % 1000000));
                } finally {
                    waiters--;
                }
            }
        }

        return result();
    }

    private CommitResult result() {
        Object result = this.result;
        if (result == null || result == CANCELLED_HOLDER) {
            return null;
        }
        return (CommitResult)result;
    }

    public void setFailure(CommitResult cause) {
        synchronized (this) {
            if (this.result == CANCELLED_HOLDER) {
                throw new IllegalStateException("future has been cancelled");
            }

            if (this.result == null) {
                this.result = cause;
                notifyWaiters();
                notifyConsumers();
                return ;
            }

            CommitResult result = (CommitResult)this.result;
            throw new IllegalStateException(String.format("the result(%s) has already set", result.name()));
        }
    }

    @Override
    public void addListener(Consumer<Commit> consumer) {
        addListener(Stage.FINISHED, consumer);
    }

    @Override
    public void addListener(Commit.Stage stage, Consumer<Commit> consumer) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(consumer, "consumer");

        synchronized (this) {
            add(new Pair(stage, consumer));
        }

        if (isDone()) {
            notifyConsumers();
        }
    }

    private void notifyConsumers() {
        int size;

        synchronized (this) {
            size = this.size();
        }

        if (size > 0) {
            for (int i = 0; i < size; i++) {
                Pair pair = get(i);
                if (pair.stage == stage) {
                    pair.consumer.accept(this);
                }
            }
        }
    }

    private void notifyWaiters() {
        synchronized (this) {
            if (waiters > 0) {
                notifyAll();
            }
        }
    }

    @Override
    public String toString() {
        return "GenericCommit{" +
            ", getType=" + type +
            ", waiters=" + waiters +
            ", begin=" + begin +
            ", done=" + done +
            ", instanceId=" + instanceId +
            ", result=" + result +
            ", stage=" + stage +
            "} ";
    }

}
