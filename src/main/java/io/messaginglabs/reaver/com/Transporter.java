package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.config.Node;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface Transporter extends Closeable {

    void useSSL();
    void init() throws Exception;
    void setConsumer(Consumer<Message> msgConsumer);
    void send(Node node, Message msg);
    boolean connect(Node node, long timeout, TimeUnit unit);
}
