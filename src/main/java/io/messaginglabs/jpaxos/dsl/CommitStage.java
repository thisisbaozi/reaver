package io.messaginglabs.jpaxos.dsl;

public enum CommitStage {

    READY,
    PREPARE,
    PROPOSED,
    CHOSEN,
    LEARNED

}
