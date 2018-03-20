package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.msg.Message;

public interface Config {

    /**
     * Returns node info of this node
     */
    Node node();

    int broadcast(Message msg);

}
