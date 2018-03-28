package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.core.Proposal;
import java.util.List;

public interface Config {

    /**
     * Returns node info of this node
     */
    Node node();

    int broadcast(Message msg);
    long begin();
    /**
     * Returns the number of acceptors in this config
     */
    int acceptors();

    void propose(long instanceId, Proposal proposal);
    void prepare(long instanceId, Proposal proposal);

    int majority();

    List<Node> members();

    ConfigView view();
}
