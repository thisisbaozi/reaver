package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.config.Node;
import io.netty.channel.ChannelFuture;
import io.netty.util.ReferenceCounted;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public interface Server extends ReferenceCounted, AutoCloseable {

    String address();

    long nodeId();

    /*
     * -_-....
     *
     * @see https://www.bilibili.com/video/av2271112
     */
    void areYouOk();

    void send(Message msg);
    void send(Message msg, long timeout) throws TimeoutException, InterruptedException;

    boolean connect(long timeout) throws InterruptedException;
    void connect();

    boolean isActive();

}
