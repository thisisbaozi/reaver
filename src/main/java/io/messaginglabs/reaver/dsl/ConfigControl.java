package io.messaginglabs.reaver.dsl;

import io.messaginglabs.reaver.config.ConfigEventsListener;
import io.messaginglabs.reaver.config.ConfigView;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.core.FollowContext;
import java.util.List;
import java.util.concurrent.Future;

public interface ConfigControl {

    ConfigView view();

    /**
     * Selects a donor from ths given member list and learn chosen values from it. The follower
     * try to match a new donor from last config view if the old donor is inactive.
     *
     * Calling {@link #join(List)} if this node wants to be an acceptor(proposer).
     */
    FollowContext follow(List<Node> nodes);

    Future<ConfigView> join(List<Node> members);
    Future<Boolean> leave();

    void add(ConfigEventsListener listener);
    void remove(ConfigEventsListener listener);

}
