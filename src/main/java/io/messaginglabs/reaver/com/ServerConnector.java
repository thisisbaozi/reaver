package io.messaginglabs.reaver.com;

import io.netty.util.ReferenceCounted;

public interface ServerConnector extends ReferenceCounted {

    Server connect(String ip, int port);

}
