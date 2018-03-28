package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.dsl.ElectionPolicy;
import io.messaginglabs.reaver.dsl.LeaderSelector;

public class GroupOptions {

    public int batch;
    public int pipeline;

    public int valueCacheCapacity;
    public int retryInterval;
    public int useConfigSpan;

    public boolean reserveConfig;
    public boolean statistics;

    /* leader */
    public boolean leaderProposeOnly;
    public int leaseDuration;
    public ElectionPolicy policy;
    public LeaderSelector selector;

    /* logging */
    public String path;
}
