package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;

public interface Proposer extends Participant {

    Commit commit(ByteBuf value);
    CommitResult commit(ByteBuf value, Object attachment);

    boolean close(long timeout);
}
