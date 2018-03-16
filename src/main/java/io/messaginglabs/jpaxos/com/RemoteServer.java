package io.messaginglabs.jpaxos.com;

import io.messaginglabs.jpaxos.utils.RefCounted;

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
}
