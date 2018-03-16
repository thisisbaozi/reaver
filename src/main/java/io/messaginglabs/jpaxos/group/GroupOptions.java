package io.messaginglabs.jpaxos.group;

public class GroupOptions {

    public int batch;
    public int pipeline;
    public int leaseDuration;
    public int valueCacheCapacity;
    public boolean leaderProposeOnly;
    public boolean statistics;

}
