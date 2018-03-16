package io.messaginglabs.jpaxos.core;

import io.messaginglabs.jpaxos.core.AbstractVoter;
import io.messaginglabs.jpaxos.core.Proposer;
import io.messaginglabs.jpaxos.dsl.Commit;
import io.messaginglabs.jpaxos.dsl.Group;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public interface SerialProposer extends Proposer {

    enum State {
        FREE,
        PROPOSING,
        READY_TO_PREPARE,
    }

    /**
     * Returns the commit this proposer is processing
     */
    ValueCommit commit();

    /**
     * Returns the state this proposer is, FREE means it's able to propose
     * a new commit, otherwise, an exception will be raised
     */
    State state();

    List<ValueCommit> newBatch();

    void commit(List<ValueCommit> batch);

    void observe(State state, Consumer<SerialProposer> consumer);

}
