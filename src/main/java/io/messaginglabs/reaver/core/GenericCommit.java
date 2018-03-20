package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
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

    public GenericCommit(ExecutorService executor, ByteBuf value, Object attachment) {
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

    @Override public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override public boolean isCancelled() {
        return false;
    }

    @Override public boolean isDone() {
        return false;
    }

    @Override public CommitResult get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override public CommitResult get(long timeout,
        TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public CommitType type() {
        return type;
    }

    @Override public long instanceId() {
        return 0;
    }

    @Override
    public void addListener(Consumer<CommitResult> consumer) {

    }

    @Override
    public void addListener(Commit.Stage stage, Consumer<Commit> consumer) {

    }
}
