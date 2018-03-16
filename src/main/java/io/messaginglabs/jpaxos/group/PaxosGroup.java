package io.messaginglabs.jpaxos.group;

import io.messaginglabs.jpaxos.com.Message;
import io.messaginglabs.jpaxos.config.Node;
import io.messaginglabs.jpaxos.dsl.Group;

public interface PaxosGroup extends Group {

    void init();
    void start();

    void boot();

    Node local();

    void process(Message msg);

    GroupEnv env();
}
