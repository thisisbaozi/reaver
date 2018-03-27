package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.core.Proposal;
import java.util.List;

public class PaxosConfig implements Config {

    private int groupId;
    private int comVersion;

    /*
     * A config
     */
    private long reconfigureId;

    /*
     * instances  should execute based on this instance id
     */
    private long beginId;

    /*
     * servers in this config, this is immutable
     */
    private Server[] servers;

    /*
     * members in this config
     */
    private List<Node> members;

    public PaxosConfig(int groupId, long reconfigureId, long beginId, List<Node> members, Server[] servers) {
        this.groupId = groupId;
        this.reconfigureId = reconfigureId;
        this.beginId = beginId;
        this.members = members;
    }

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

    @Override public void propose(long instanceId, Proposal proposal) {

    }

    @Override public void prepare(long instanceId, Proposal proposal) {

    }

    @Override public int majority() {
        return 0;
    }

    @Override public long begin() {
        return 0;
    }
}
