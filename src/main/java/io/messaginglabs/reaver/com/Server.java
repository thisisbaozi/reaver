package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.config.Node;
import io.netty.util.ReferenceCounted;
import java.util.function.Consumer;

public interface Server extends ReferenceCounted {

    enum State {
        Inactive, Active,
    }

    boolean join(Node node);

    /*
     * -_-....
     *
     * @see https://www.bilibili.com/video/av2271112
     */
    void areYouOk();


    boolean isOk();

    /**
     * Adds a consumer to this server, this consumer is notified when this
     * server's state is changed.
     */
    void observe(Consumer<Server.State> consumer);

    /**
     * Returns the last time when accessed this server
     */
    long time();
}
