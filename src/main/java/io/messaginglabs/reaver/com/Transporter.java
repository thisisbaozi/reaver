package io.messaginglabs.reaver.com;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.util.ReferenceCounted;
import java.util.function.Consumer;

public interface Transporter extends AutoCloseable, ReferenceCounted {

    void init() throws Exception;
    void setConsumer(Consumer<ByteBuf> msgConsumer);

    ChannelFuture connect(String ip, int port);
}
