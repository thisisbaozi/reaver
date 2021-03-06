package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.dsl.Commit;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;

public interface Proposer extends Participant {

    void init() throws Exception;

    Sequencer sequencer();
    InstanceCache cache();

    Commit commit(ByteBuf value);
    CommitResult commit(ByteBuf value, Object attachment);

    boolean close(long timeout);

    void process(Message msg);
}
