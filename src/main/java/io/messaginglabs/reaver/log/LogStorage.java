package io.messaginglabs.reaver.log;

import io.netty.util.ReferenceCounted;
import java.io.Closeable;

public interface LogStorage extends Closeable, ReferenceCounted {

    void init() throws Exception;

}
