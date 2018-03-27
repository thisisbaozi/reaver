package io.messaginglabs.reaver.dsl;

import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.log.LogStorage;

public class PaxosOptions {

    /* rpc */
    public Node node;

    /* performance */
    public int batch;
    public int pipeline;
    public int valuesCache;

    /* storage */
    public String path;
    public LogStorage storage;

    /* threading model */
    public ThreadingModel model;
    public int executorPoolSize;

    /* statistics */
    public boolean enableStatistics;

    /* leader */
    public boolean leaderProposeOnly;
    public int leaseDuration;
    public ElectionPolicy policy;
    public LeaderSelector selector;

}
