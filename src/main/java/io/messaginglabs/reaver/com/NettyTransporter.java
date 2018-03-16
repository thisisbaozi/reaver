package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.utils.Parameters;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NettyTransporter implements Transporter {

    public NettyTransporter(String address, int port, int threads, String prefix) {
        Parameters.checkNotEmpty(address, "address");

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
}
