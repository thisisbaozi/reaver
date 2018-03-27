package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.config.Node;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import java.util.function.Consumer;

public class RemoteServer extends AbstractReferenceCounted implements Server {

    private final String ip;
    private final int port;

    public RemoteServer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    protected void deallocate() {

    }

    @Override
    public boolean join(Node node) {
        return false;
    }

    @Override public boolean isOk() {
        return false;
    }

    @Override
    public void observe(Consumer<State> observer) {

    }

    @Override public long time() {
        return 0;
    }

    @Override public void areYouOk() {

    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }
}
