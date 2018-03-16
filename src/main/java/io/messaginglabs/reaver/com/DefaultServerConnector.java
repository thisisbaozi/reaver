package io.messaginglabs.reaver.com;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultServerConnector implements ServerConnector {

    private final int count;
    private final Map<String, List<Server>> servers;

    public DefaultServerConnector(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be 0 or positive number, but given: " + count);
        }

        this.count = count;
        this.servers = new HashMap<>();
    }

    @Override
    public Server connect(String ip, int port) {
        String address = String.format("%s:%d", ip, port);

        synchronized (this) {
            List<Server> servers = this.servers.computeIfAbsent(address, k -> new ArrayList<>());

            Server server = null;
            if (count == 0) {
                /*
                 * each group has a exclusive server
                 */
                server = new RemoteServer(ip, port);
            } else {
                /*
                 * finds one from active servers
                 */
                for (Server active : servers) {
                    if (active.refCount() < count) {
                        server = active;
                        break;
                    }
                }

                if (server == null) {
                    server = new RemoteServer(ip, port);
                    servers.add(server);
                }
            }

            server.retain();
            return server;
        }
    }

}
