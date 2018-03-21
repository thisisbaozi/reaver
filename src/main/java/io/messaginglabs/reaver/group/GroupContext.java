package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.core.InstanceCache;

public interface GroupContext {

    long maxInstanceId(long instanceId);
    long maxInstanceId();

    /**
     * Returns the number of instances that are not applied to state machine.
     */
    int pendingCompletedInstances();

    InstanceCache cache();

    GroupEnv env();

}
