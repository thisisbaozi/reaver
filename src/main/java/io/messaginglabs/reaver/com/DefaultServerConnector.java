package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.utils.Parameters;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultServerConnector extends AbstractReferenceCounted implements ServerConnector {

    private static final Logger logger = LoggerFactory.getLogger(DefaultServerConnector.class);

    private final int count;
    private final Map<String, List<Server>> servers;
    private final boolean debug;
    private Transporter transporter;

    public DefaultServerConnector(int count, boolean debug, Transporter transporter) {
        this.count = Parameters.requireNotNegative(count, "count");
        this.debug = debug;
        this.transporter = Objects.requireNonNull(transporter, "transporter");
        this.servers = new HashMap<>();
    }

    @Override
    protected void deallocate() {
        synchronized (this) {
            servers.values().forEach(servers -> servers.forEach(server -> {
                if (server.refCnt() > 0 && logger.isWarnEnabled()) {
                    logger.warn("no one will rely on connector, but the count（{}） of ref of server({}) is not 0", server.refCnt(), server.toString());
                }
            }));
        }
    }

    @Override
    public Server connect(String ip, int port) {
        String address = String.format("%s:%d", ip, port);

        synchronized (this) {
            List<Server> servers = this.servers.computeIfAbsent(address, k -> new ArrayList<>());

            Server server = null;

            if (count > 0) {
                for (Server active : servers) {
                    if (active.refCnt() < count) {
                        server = active;
                        break;
                    }
                }
            }

            if (server == null) {
                server = new RemoteServer(ip, port, debug, transporter) {
                    @Override
                    protected void deallocate() {
                        closeServer(this);
                    }
                };
                servers.add(server);
            } else {
                server.retain();
            }

            return server;
        }
    }

    private void closeServer(Server server) {
        String address = server.address();

        if (server.refCnt() != 0) {
            throw new IllegalStateException(
                String.format("server(%s) is still usable(%d)", address, server.refCnt())
            );
        }

        try {
            server.close();
        } catch (Exception cause) {
            /*
             * Likely, it's impossible
             */
            logger.warn("can't close server({})", address, cause);
        }

        synchronized (this) {
            List<Server> active = servers.get(address);
            active.remove(server);

            if (logger.isInfoEnabled()) {
                logger.info("nobody relies on server({}), close and remove it, remain {}", address, active.size());
            }
        }
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

}
