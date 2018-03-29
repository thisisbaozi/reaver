package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.config.Node;
import io.netty.channel.ChannelFuture;
import io.netty.util.ReferenceCounted;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface Transporter extends Closeable, ReferenceCounted {

    void useSSL();
    void init() throws Exception;
    void setConsumer(Consumer<Message> msgConsumer);
    void send(Node node, Message msg);
    boolean connect(Node node, long timeout, TimeUnit unit);

    ChannelFuture connect(String ip, int port);
}
