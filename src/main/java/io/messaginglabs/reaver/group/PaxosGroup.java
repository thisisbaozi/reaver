package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.Message;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.dsl.Group;

public interface PaxosGroup extends Group {

    void init();
    void start();

    void boot();

    Node local();

    void process(Message msg);

    GroupEnv env();
}