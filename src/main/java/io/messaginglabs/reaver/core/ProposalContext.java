package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.config.Config;
import io.messaginglabs.reaver.dsl.Commit;
import java.util.List;

public class ProposalContext {

    private long begin;
    private long sequence;
    private List<GenericCommit> batch;
    private int delayed = 0;
    private Commit.Stage stage;
    private Config config;

    public void reset(long sequence, List<GenericCommit> batch, Config config) {
        this.sequence = sequence;
        this.batch = batch;
        this.begin = System.currentTimeMillis();
    }

    public long sequence() {
        return sequence;
    }

    public int countDelay() {
        delayed++;
        return delayed - 1;
    }

}
