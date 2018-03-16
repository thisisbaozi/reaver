package io.messaginglabs.jpaxos.dsl;

import io.messaginglabs.jpaxos.config.ConfigControl;
import java.nio.ByteBuffer;

public interface Group {

    int id();

    Commit commit(ByteBuffer value);

    ConfigControl config();
    GroupStatistics statistics();

    void close();

    void destroy();

    boolean isClosed();

}