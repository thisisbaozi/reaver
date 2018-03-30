package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.config.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.util.ReferenceCounted;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface Transporter extends AutoCloseable, ReferenceCounted {

    void init() throws Exception;
    void setConsumer(Consumer<ByteBuf> msgConsumer);

    ChannelFuture connect(String ip, int port);
}
