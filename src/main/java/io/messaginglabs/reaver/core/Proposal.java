package io.messaginglabs.reaver.core;

import io.netty.buffer.ByteBuf;

public class Proposal extends Ballot {

    private ByteBuf value;


    public ByteBuf getValue() {
        return value;
    }

    public void setValue(ByteBuf value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Proposal{" +
            "sequence=" + sequence +
            ", nodeId=" + nodeId +
            ", value=" + value.readableBytes() +
            '}';
    }
}
