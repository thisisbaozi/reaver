package io.messaginglabs.reaver.core;

import io.netty.buffer.ByteBuf;

public class Proposal {

    private int sequence;

    /*
     * the node proposed this proposal
     */
    private long nodeId;

    /*
     * the value the proposer wants to propose
     */
    private ByteBuf value;

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

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
