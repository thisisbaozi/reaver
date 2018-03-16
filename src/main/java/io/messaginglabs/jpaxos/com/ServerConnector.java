package io.messaginglabs.jpaxos.com;

public interface ServerConnector {


    Server connect(String ip, int port);

}
