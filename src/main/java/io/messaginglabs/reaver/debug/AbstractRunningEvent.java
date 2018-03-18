package io.messaginglabs.reaver.debug;

public abstract class AbstractRunningEvent implements RunningEvent {

    private final long time;
    private final Thread thread;

    public AbstractRunningEvent() {
        this.time = System.nanoTime();
        this.thread = Thread.currentThread();
    }

    @Override
    public long time() {
        return time;
    }

    @Override
    public Thread thread() {
        return thread;
    }

}
