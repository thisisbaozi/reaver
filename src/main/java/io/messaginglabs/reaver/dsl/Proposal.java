package io.messaginglabs.reaver.dsl;

public interface Proposal {

    enum State {
        INIT,
        PENDING,
        CHOSEN,
        FAIL,
    }


}
