package io.messaginglabs.reaver.core;

import io.netty.buffer.ByteBuf;

public interface Applier extends AutoCloseable {


    void apply(PaxosInstance instance);

}
