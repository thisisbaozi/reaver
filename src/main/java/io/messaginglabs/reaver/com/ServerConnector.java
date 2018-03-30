package io.messaginglabs.reaver.com;

import io.netty.util.ReferenceCounted;
import java.util.List;

public interface ServerConnector extends ReferenceCounted {

    Server connect(String ip, int port);

    List<Server> get(String ip, int port);
}
