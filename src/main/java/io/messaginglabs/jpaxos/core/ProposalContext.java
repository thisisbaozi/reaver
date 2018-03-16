package io.messaginglabs.jpaxos.core;

import io.messaginglabs.jpaxos.dsl.Commit;
import io.messaginglabs.jpaxos.dsl.CommitStage;
import java.util.List;

public class ProposalContext {

    private long begin;
    private long sequence;
    private List<ValueCommit> batch;
    private int delayed = 0;
    private CommitStage stage;

    public void reset(long sequence, List<ValueCommit> batch) {
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
