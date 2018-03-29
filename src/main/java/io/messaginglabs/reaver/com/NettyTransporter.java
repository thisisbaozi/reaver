package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.utils.Parameters;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NettyTransporter extends AbstractReferenceCounted implements Transporter {

    private Bootstrap bootstrap;

    public NettyTransporter(String address, int port, int threads, String prefix) {
        Parameters.requireNotEmpty(address, "address");

        if (port <= 0) {
            throw new IllegalArgumentException("invalid port");
        }

        if (threads <= 0) {
            throw new IllegalArgumentException("the number of io threads must be greater than 0, but given: " + threads);
        }

    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public ChannelFuture connect(String ip, int port) {
        return bootstrap.connect(new InetSocketAddress(ip, port));
    }

    @Override
    public void useSSL() {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void setConsumer(Consumer<Message> msgConsumer) {

    }

    @Override public void send(Node node, Message msg) {

    }

    @Override public boolean connect(Node node, long timeout, TimeUnit unit) {
        return false;
    }

    @Override protected void deallocate() {

    }

    @Override public ReferenceCounted touch(Object hint) {
        return null;
    }
}
