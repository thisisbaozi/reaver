package io.messaginglabs.reaver.dsl;

import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.log.LogStorage;

public interface PaxosBuilder {

    /* statistics */
    void setEnableStatistics(boolean enable);

    /* performance */
    void setBatchSize(int maxBatchSize);
    void setPipeline(int pipeline);
    void setValueCacheCapacity(int capacity);

    // storage
    void setDir(String path);
    void setLogStorage(LogStorage storage);

    /* lease */
    void setElectionPolicy(ElectionPolicy policy);
    void setLeaderSelector(LeaderSelector selector);
    void setLeaderProposeOnly(boolean enable);
    void setLeaseDuration(int duration);

    /* com */
    void setNode(Node node);

    PaxosGroup build(StateMachine stateMachine) throws Exception;

}
