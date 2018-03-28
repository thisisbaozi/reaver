package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.config.ConfigView;
import io.messaginglabs.reaver.config.Node;
import java.util.concurrent.Future;

public interface FollowContext {

    /**
     * Returns the node providing chosen values to this node
     */
    Node provider();

    /**
     * Returns last config this follower learned
     */
    ConfigView view();

    /**
     * Stops following the current provider(it might be in trouble) and
     * follows the given member
     */
    Future<Boolean> redirect(Node member);

}
