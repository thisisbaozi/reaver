package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.dsl.CommitStage;
import io.messaginglabs.reaver.dsl.Proposal;
import io.netty.buffer.ByteBuf;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class GenericCommit implements Commit {

    private final ExecutorService executor;
    private final ByteBuf value;

    /*
     * a failed commit
     */
    private CommitResult result;

    private CommitType type;

    public GenericCommit(ExecutorService executor, ByteBuf value) {
        this.executor = executor;
        this.value = value;
    }

    public int getValueSize() {
        return value.readableBytes();
    }

    public void setFailure(CommitResult result) {

    }

    public boolean markNotCancellable() {
return false;
    }

    @Override public long begin() {
        return 0;
    }

    @Override public long proposed() {
        return 0;
    }

    @Override public long resolved() {
        return 0;
    }

    @Override public long learned() {
        return 0;
    }

    @Override public void observe(CommitStage stage, Consumer<Proposal> consumer) {

    }

    @Override public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override public boolean isCancelled() {
        return false;
    }

    @Override public boolean isDone() {
        return false;
    }

    @Override public Proposal get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public Proposal get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public CommitType type() {
        return type;
    }
}
