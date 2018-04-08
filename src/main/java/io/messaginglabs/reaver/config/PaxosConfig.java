package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.msg.Message;

public interface PaxosConfig {

    /**
     * Returns the instance id that this config takes effect, taking effect
     * means the proposer can use this config to propose values.
     *
     * +-----+------+--------+------+-----+
     * | ... | i(n) | ...(x) | i(m) | ... |
     * +-----+------+--------+------+-----+
     *
     * n is the instance to reconfigure
     * m is the instance this config can be used to reach a agreement on any value.
     */
    long begin();
    long instanceId();

    int majority();

    void broadcast(Message msg);

    boolean isMember(long nodeId);

    Server find(long nodeId);

    /**
     * Returns the members in this config, they are acceptors instead of
     * learners, followers or proposers.
     */
    Member[] members();
    Server[] servers();

}
