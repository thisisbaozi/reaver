package io.messaginglabs.reaver.dsl;

import io.messaginglabs.reaver.config.ConfigEventsListener;
import io.messaginglabs.reaver.config.ConfigView;
import io.messaginglabs.reaver.config.Node;
import java.util.List;

public interface ConfigControl {

    ConfigView view();

    /**
     * Selects a donor from ths given member list and learn chosen values from it. The follower
     * try to find a new donor from last config view if the old donor is inactive.
     *
     * Calling {@link #join(List)} if this node wants to be an acceptor(proposer).
     */
    void follow(List<Node> nodes);

    void join(List<Node> nodes);
    void leave();

    void add(ConfigEventsListener listener);
    void remove(ConfigEventsListener listener);

}
