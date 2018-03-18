package io.messaginglabs.reaver.core;

import java.util.List;
import java.util.function.Consumer;

public interface SerialProposer extends Voter {

    enum State {
        FREE,
        PROPOSING,
        READY_TO_PREPARE,
    }

    /**
     * Returns the state this proposer is, FREE means it's able to propose
     * a new commit, otherwise, an exception will be raised
     */
    State state();
    List<GenericCommit> newBatch();

    void commit(List<GenericCommit> batch);

    void observe(State state, Consumer<SerialProposer> consumer);

}
