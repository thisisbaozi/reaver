package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.config.GroupConfigs;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.core.InstanceCache;
import io.messaginglabs.reaver.core.Sequencer;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.dsl.StateMachine;

public interface InternalPaxosGroup extends PaxosGroup {

    Node local();

    void process(Message msg);

    GroupEnv env();
    GroupOptions options();
    GroupConfigs configs();

    long maxInstanceId(long instanceId);
    long maxInstanceId();

    boolean isSlowDown(long instanceId);

    void freeze(String msg);

    /**
     * Returns the number of instances that are not applied to state machine.
     */
    int pendingCompletedInstances();

    InstanceCache cache();

    void addCloseListener(Runnable runner);
    StateMachine getStateMachine();

    GroupContext ctx();
    Sequencer sequencer();
}
