package io.messaginglabs.reaver.com.msg;

import io.netty.buffer.ByteBuf;

public class CommitValue extends Message {

    // proposal
    private int sequence;
    private long nodeId;

    // Paxos instance
    private long instanceId;

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

    public long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(long instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    protected int bodySize() {
        return 20;
    }

    @Override
    protected void decodeBody(ByteBuf buf) {
        buf.writeLong(instanceId);
        buf.writeInt(sequence);
        buf.writeLong(nodeId);
    }

    @Override
    protected void encodeBody(ByteBuf buf) {
        instanceId = buf.readLong();
        sequence = buf.readInt();
        nodeId = buf.readLong();
    }
}
