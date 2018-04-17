package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.com.msg.Message;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class LocalServer extends AbstractReferenceCounted implements Server {

    private final Consumer<Message> consumer;
    private final long localId;
    private final String address;

    public LocalServer(long localId, String address, Consumer<Message> consumer) {
        this.consumer = consumer;
        this.localId = localId;
        this.address = address;
    }

    @Override
    public void send(Message msg) {
        if (consumer != null) {
            consumer.accept(msg);
        }
    }

    @Override
    public void send(Message msg, long timeout) throws TimeoutException, InterruptedException {
        send(msg);
    }

    @Override
    public boolean connect(long timeout) throws InterruptedException {
        return true;
    }

    @Override
    public String address() {
        return address;
    }

    @Override
    public long nodeId() {
        return localId;
    }

    @Override
    public void connect() {
        // do nothing
    }

    @Override
    public void areYouOk() {
        // do nothing
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    protected void deallocate() {
        // do nothing
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

    @Override
    public void close() throws Exception {
        // do nothing
    }

}
