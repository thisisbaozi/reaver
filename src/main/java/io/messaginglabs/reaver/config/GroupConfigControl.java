package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.core.FollowContext;
import io.messaginglabs.reaver.dsl.CheckpointStateMachine;
import io.messaginglabs.reaver.dsl.ConfigControl;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import io.messaginglabs.reaver.utils.ContainerUtils;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupConfigControl implements ConfigControl {

    private static final Logger logger = LoggerFactory.getLogger(GroupConfigControl.class);

    private final InternalPaxosGroup group;
    private final GroupConfigs configs;

    private boolean hasJoined = false;

    // promises
    private CompletableFuture<ConfigView> future;

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
    public FollowContext follow(List<Node> nodes) {
        //follow(nodes, Defines.VOID_INSTANCE_ID);
        return null;
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
    public Future<ConfigView> join(List<Node> members) {
        synchronized (this) {
            Config config = configs.newest();
            if (config != null) {
                logger.info("this node({}) has already joined the group({})", ContainerUtils.toString(config.members(), "members"), group.id());

                if (future != null) {
                    return future;
                }

                future = new CompletableFuture<>();
                future.complete(config.view());

                return future;
            }

            return doJoin(members);
        }
    }

    private Future<ConfigView> doJoin(List<Node> members) {
        isValid(members);

        if (logger.isInfoEnabled()) {
            logger.info(
                "this node({}) try to join the group({}) based on the given donors({})",
                group.local().toString(),
                group.id(),
                ContainerUtils.toString(members, "members")
            );
        }

        if (future != null) {
            return future;
        }

        boolean result = false;
        for (Node member : members) {
            if (!member.equals(group.local()) && join(member)) {
                result = true;
                logger.info("join the group({}) through the member({})", group.id(), member.toString());

                break;
            }

            logger.info("can't join the group({}) through the member({})", group.id(), member.toString());
        }

        if (result) {
            hasJoined = true;
        } else {
            throw new IllegalStateException(
                String.format("can't join the group(%d) through %s", group.id(), ContainerUtils.toString(members, "members"))
            );
        }

        return (future = new CompletableFuture<>());
    }

    private boolean join(Node node) {
        /*
         * connect with the given node and send it a message that this node
         * want to join the group
         */
        Server server = group.env().connector.connect(node.getIp(), node.getPort());
        //boolean result = server.join(group.id(), group.local());

        /*
         * if others rely on this server, they should connect with it by themselves
         */
        server.release();

        return false;
    }

    @Override
    public Future<Boolean> leave() {
        return null;
    }

    @Override
    public void add(ConfigEventsListener listener) {

    }

    @Override
    public void remove(ConfigEventsListener listener) {

    }
}
