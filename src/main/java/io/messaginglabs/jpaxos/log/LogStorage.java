package io.messaginglabs.jpaxos.log;

import java.io.Closeable;

public interface LogStorage extends Closeable {


    void init() throws Exception;

}
