package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.utils.RefCount;
import java.util.function.Consumer;

public interface Server extends RefCount {

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

}
