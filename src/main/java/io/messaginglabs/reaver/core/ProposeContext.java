package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.config.Config;
import java.util.List;
import java.util.Objects;

public class ProposeContext {

    /*
     * begin: when this proposal proposed
     * end: when this proposal completed
     */
    private long begin;
    private long end;

    /*
     * times this proposal proposed due to conflicts or other
     * reasons(e.g: network trouble).
     */
    private int times;

    // the id of Paxos instance this proposal associated with
    private long instanceId;

    // the instance propose this proposal based on the config
    private Config config;
    private AlgorithmPhase phase;
    private List<GenericCommit> value;

    private final Ballot maxPromised = new Ballot();
    private final VotersCounter prepareCounter = new VotersCounter();
    private final VotersCounter acceptCounter = new VotersCounter();

    public void reset(long instanceId, List<GenericCommit> batch, Config config) {
        this.instanceId = instanceId;
        this.value = batch;
        this.config = Objects.requireNonNull(config, "config");
        this.begin = System.currentTimeMillis();
        this.end = 0;
        this.times = 0;
    }

    public int countDelay() {
        times++;
        return times - 1;
    }

    public AlgorithmPhase phase() {
        return phase;
    }

    public Config config() {
        return config;
    }

    public long instanceId() {
        return 0;
    }

    public Ballot maxPromised() {
        return maxPromised;
    }
}
