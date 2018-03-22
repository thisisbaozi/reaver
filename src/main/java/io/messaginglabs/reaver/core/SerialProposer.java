package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.PrepareReply;
import io.messaginglabs.reaver.com.msg.ProposeReply;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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
    void process(PrepareReply reply);
    void process(ProposeReply reply);
    void observe(State state, Consumer<SerialProposer> consumer);

    void close();
    boolean isClosed();
}
