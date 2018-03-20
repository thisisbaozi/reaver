package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.dsl.ElectionPolicy;
import io.messaginglabs.reaver.dsl.LeaderSelector;

public class GroupOptions {

    public int batch;
    public int pipeline;

    public int valueCacheCapacity;

    public boolean statistics;

    /* leader */
    public boolean leaderProposeOnly;
    public int leaseDuration;
    public ElectionPolicy policy;
    public LeaderSelector selector;
}
