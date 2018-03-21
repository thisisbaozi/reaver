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

    void setDelay(int time);
    void setProposeBeforeHook(Function<ProposeContext, Boolean> processor);

    /**
     * Returns the state this proposer is, FREE means it's able to propose
     * a new commit, otherwise, an exception will be raised
     */
    State state();
    List<GenericCommit> newBatch();

    void commit(List<GenericCommit> batch);
    void process(PrepareReply reply);
    void process(ProposeReply reply);
    void observe(State state, Consumer<SerialProposer> consumer);

    void close();
}
