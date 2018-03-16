package io.messaginglabs.jpaxos.dsl;

public interface Proposal {

    enum State {
        INIT,
        PENDING,
        CHOSEN,
        FAIL,
    }


}
