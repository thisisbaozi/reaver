package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.core.Defines;
import io.messaginglabs.reaver.dsl.CheckpointStateMachine;
import io.messaginglabs.reaver.dsl.ConfigControl;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupConfigControl implements ConfigControl {

    private static final Logger logger = LoggerFactory.getLogger(GroupConfigControl.class);

    private final InternalPaxosGroup group;
    private final GroupConfigs configs;

    private boolean hasJoined = false;

    public GroupConfigControl(InternalPaxosGroup group) {
        this.group = group;
        this.configs = group.configs();
    }

    @Override
    public ConfigView view() {
        return null;
    }

    private static void isValid(List<Node> nodes) {
        Objects.requireNonNull(nodes);

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("empty node set");
        }
    }

    @Override
    public void follow(List<Node> nodes) {
        follow(nodes, Defines.VOID_INSTANCE_ID);
    }

    @Override
    public void follow(List<Node> nodes, long id) {
        isValid(nodes);

        if (hasJoined) {
            throw new IllegalStateException(
                String.format("this node has joined a group(%d)", group.id())
            );
        }

        // find a donor(a learner) from the given list if there's no a available config
        //
        long beginId = resolveBeginId(id);

    }

    private long resolveBeginId(long id) {
        if (id > 0) {
            return id;
        }

        StateMachine sm = group.getStateMachine();
        if (sm == null) {
            throw new IllegalStateException(
                String.format("no state machine register to group(%d)", group.id())
            );
        }

        if (sm instanceof CheckpointStateMachine) {
            id = ((CheckpointStateMachine)sm).getCheckpoint();
        }

        if (id <= 0) {
            // this follower is a new node, dump completed instances
            // from 1 to last one.
            id = 1;
        }

        return id;
    }

    @Override
    public void join(List<Node> nodes) {
        isValid(nodes);

        if (hasJoined) {
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
