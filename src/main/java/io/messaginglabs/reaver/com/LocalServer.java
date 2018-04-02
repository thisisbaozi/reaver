package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalServer extends AbstractReferenceCounted implements Server {

    private static final Logger logger = LoggerFactory.getLogger(LocalServer.class);
    private final InternalPaxosGroup group;

    public LocalServer(InternalPaxosGroup group) {
        this.group = Objects.requireNonNull(group, "group");
    }

    @Override
    public void send(Message msg) {
        ScheduledExecutorService executor = group.env().executor;
        try {
            executor.execute(() -> group.process(msg));
        } catch (Exception cause) {
            if (logger.isWarnEnabled()) {
                logger.warn("executor({}) of group({}) can't execute task", Boolean.toString(executor.isShutdown()), group.id());
            }
        }
    }

    @Override
    public String address() {
        return group.local().toString();
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
        return group.state() == PaxosGroup.State.RUNNING;
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
