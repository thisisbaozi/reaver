package io.messaginglabs.reaver.dsl;

import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface Commit extends Future<Proposal> {

    long begin();

    long proposed();

    long resolved();

    long learned();

    void observe(CommitStage stage, Consumer<Proposal> consumer);

}
