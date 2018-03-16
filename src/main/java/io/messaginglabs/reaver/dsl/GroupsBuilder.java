package io.messaginglabs.reaver.dsl;

public interface GroupsBuilder {

    void setPrefix(String prefix);

    /* executor */
    void setExecutorThreads(int num);
    void setUseExclusiveAlgorithmExecutor(boolean enable);

    /* statistics */
    void setEnableStatistics(boolean enable);

    /* performance */
    void setBatchSize(int maxBatchSize);
    void setPipeline(int pipeline);
    void setValueCacheCapacity(int capacity);

    // storage
    void setDir(String path);

    /* lease */
    void setLeaderProposeOnly(boolean enable);
    void setLeaseDuration(int duration);

    /* communication */
    void setPort(int port);
    void setIoThreads(int num);
    void setLocalAddress(String address);

    /*
     * Builds a new group based on options
     */
    Group build(int id) throws Exception;
    Group build(int id, boolean debug) throws Exception;
    Group build(int id, boolean debug, String path) throws Exception;
    Group build(int id, boolean debug, String path, boolean exclusive, String prefix) throws Exception;

}
