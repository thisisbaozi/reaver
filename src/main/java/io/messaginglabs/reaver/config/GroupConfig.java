package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.msg.Message;
import java.util.List;

public class GroupConfig implements Config {

    private int groupId;

    /*
     * A config
     */
    private long instanceId;

    /*
     * instances  should execute based on this instance id
     */
    private long beginInstanceId;

    /*
     * servers in this config, this is immutable
     */
    private Server[] servers;

    /*
     * acceptors in this config
     */
    private List<Node> acceptors;

    @Override
    public Node node() {
        return null;
    }

    @Override
    public int broadcast(Message msg) {
        return 0;
    }

    @Override public int acceptors() {
        return 0;
    }
}
