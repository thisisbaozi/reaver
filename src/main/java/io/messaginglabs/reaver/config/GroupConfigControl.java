package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.group.PaxosGroup;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupConfigControl implements ConfigControl {

    private static final Logger logger = LoggerFactory.getLogger(GroupConfigControl.class);

    private final PaxosGroup group;

    public GroupConfigControl(PaxosGroup group) {
        this.group = group;
    }

    @Override
    public ConfigView view() {
        return null;
    }

    @Override
    public void join(List<Node> nodes) {
        Objects.requireNonNull(nodes, "nodes");

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("no nodes");
        }

        if (hasJoined()) {
            throw new IllegalStateException(
                String.format("this node has joined a group(%d)", group.id())
            );
        }

        /*
         * Either this node is a new one of a group and others are active, or
         * it's the boot node(first one to start)
         */
        boolean joined = false;
        for (Node node : nodes) {
            if (node.equals(group.local())) {
                continue;
            }

            if (add(node)) {
                joined = true;
                logger.info("node({}) is response for adding this node({}, group({}))", node.toString(), group.local().toString(), group.id());

                break;
            }
        }

        if (!joined) {
            /*
             * this node is the boot node of the group?
             */
            logger.info("can't join the group through nodes({}), this node could be boot node", Nodes.dump(nodes));
            group.boot();
        }
    }

    private boolean add(Node node) {
        /*
         * connect with the given node and send it a message that this node
         * want to join the group
         */
        Server server = group.env().connector.connect(node.getIp(), node.getPort());
        boolean result = server.join(group.local());

        /*
         * if others rely on this server, they should connect with it by themselves
         */
        server.release();

        return result;
    }

    private boolean hasJoined() {
        return false;
    }

    @Override
    public void leave() {

    }

    @Override
    public void add(ConfigEventsListener listener) {

    }

    @Override
    public void remove(ConfigEventsListener listener) {

    }
}
