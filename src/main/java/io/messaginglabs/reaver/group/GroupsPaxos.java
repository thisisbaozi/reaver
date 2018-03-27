package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.dsl.PaxosOptions;

public interface GroupsPaxos extends AutoCloseable {

    void init(PaxosOptions options) throws Exception;

    int size();

    PaxosGroup get(int id);

    default PaxosGroup create(int id) throws Exception {
        return create(id, false);
    }

    PaxosGroup create(int id, boolean debug);

}
