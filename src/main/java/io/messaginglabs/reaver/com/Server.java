package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.config.Node;
import io.netty.util.ReferenceCounted;
import java.util.function.Consumer;

public interface Server extends ReferenceCounted, AutoCloseable {

    /*
     * -_-....
     *
     * @see https://www.bilibili.com/video/av2271112
     */
    void areYouOk();

    void send(Message msg);

    String address();

}
