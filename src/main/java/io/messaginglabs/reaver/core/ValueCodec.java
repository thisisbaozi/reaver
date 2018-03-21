package io.messaginglabs.reaver.core;

import io.netty.buffer.ByteBuf;
import java.util.List;

public interface ValueCodec {

    void encode(List<GenericCommit> commits, ByteBuf dst);

}
