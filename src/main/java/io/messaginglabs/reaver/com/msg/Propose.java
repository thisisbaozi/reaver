package io.messaginglabs.reaver.com.msg;

import io.netty.buffer.ByteBuf;

public class Propose extends Message {

    private int proposerId;
    private int sequence;
    private long nodeId;
    private long instanceId;

    /*
     * combined myValue
     */
    private ByteBuf value;

    public int getProposerId() {
        return proposerId;
    }

    public void setProposerId(int proposerId) {
        this.proposerId = proposerId;
    }

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

    public ByteBuf getValue() {
        return value;
    }

    public void setValue(ByteBuf value) {
        this.value = value;
    }

    @Override
    protected int bodySize() {
        return 0;
    }

    @Override
    protected void decodeBody(ByteBuf buf) {
        super.decodeBody(buf);
    }

    @Override
    protected void encodeBody(ByteBuf buf) {
        super.encodeBody(buf);
    }
}
