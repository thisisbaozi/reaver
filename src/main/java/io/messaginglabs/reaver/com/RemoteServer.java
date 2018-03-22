package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.utils.RefCounted;
import java.util.function.Consumer;

public class RemoteServer extends RefCounted implements Server {

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

    @Override public void areYouOk() {

    }

}
