package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.dsl.CommitStage;
import java.util.List;

public class ProposalContext {

    private long begin;
    private long sequence;
    private List<GenericCommit> batch;
    private int delayed = 0;
    private CommitStage stage;

    public void reset(long sequence, List<GenericCommit> batch) {
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

    public CommitStage stage() {
        return stage;
    }

    public CommitStage stage(CommitStage stage) {
        CommitStage current = this.stage;
        this.stage = stage;
        return current;
    }
}
