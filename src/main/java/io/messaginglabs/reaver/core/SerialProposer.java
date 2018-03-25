package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.AcceptorReply;
import java.util.List;
import java.util.function.Consumer;

public interface SerialProposer {

    enum State {
        FREE,
        PROPOSING,
        READY_TO_PREPARE,
    }

    /**
     * Returns true iff this proposer is processing a proposal, otherwise returns
     * false.
     */
    boolean isBusy();
    List<GenericCommit> valueCache();

    void commit(List<GenericCommit> batch);
    void process(AcceptorReply reply);
    void observe(State state, Consumer<SerialProposer> consumer);

    void close();
    boolean isClosed();
}
