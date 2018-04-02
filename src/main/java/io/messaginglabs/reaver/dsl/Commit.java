package io.messaginglabs.reaver.dsl;

import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface Commit extends Future<CommitResult> {

    enum Stage {
        READY,
        PREPARE,
        PROPOSED,
        CHOSEN,
        LEARNED,
        FINISHED,
    }

    Stage stage();

    /**
     * Returns the Paxos instance containing this commit, a Paxos instance
     * might be composed multiple commits. returns -1 if the commit is still
     * pending or failed.
     */
    long instanceId();

    /**
     * Adds a listener to this commit, the listener is notified when this
     * commit is done. if this commit is already completed, it's notified
     * immediately.
     */
    void addListener(Consumer<Commit> consumer);

    /**
     * Adds a listener to this commit, this listener is notified when the commit
     * reaches to the specified stage.
     */
    void addListener(Commit.Stage stage, Consumer<Commit> consumer);

}
