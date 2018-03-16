package io.messaginglabs.jpaxos.dsl;

import io.messaginglabs.jpaxos.log.LogStorage;

public class GroupOptions {

    public boolean debug = false;

    public int maxBatchSize;
    public int parallel;

    /*
     * threads
     */
    public int ioThreads;
    public int applierThreads;
    public int executorThreads;

    public boolean runWithOneGroupOnThread = false;
    public boolean enableStatistics = true;
    public boolean leaderProposeOnly = true;
    public boolean compression = false;
    public boolean enableSSL = false;
    public boolean useCheckpoint = false;

    public int electionTerm = 60;
    public int electionSuspect = 45;

    public LogStorage storage = null;

}
