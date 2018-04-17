package io.messaginglabs.reaver.com.msg;

import io.netty.buffer.ByteBuf;

public class AcceptorReply extends Message {

    // proposal
    private int sequence;
    private long nodeId;
    private ByteBuf value;

    private int replySequence;

    private long instanceId;
    private long acceptorId;

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

    public long getAcceptorId() {
        return acceptorId;
    }

    public void setAcceptorId(long acceptorId) {
        this.acceptorId = acceptorId;
    }

    public int getReplySequence() {
        return replySequence;
    }

    public void setReplySequence(int replySequence) {
        this.replySequence = replySequence;
    }

    @Override
    protected int bodySize() {
        return 0;
    }

    @Override
    public String toString() {
        return "AcceptorReply{" +
            "getOp=" + getOp().name() +
            ", groupId=" + getGroupId() +
            ", sequence=" + sequence +
            ", nodeId=" + nodeId +
            ", myValue=" + value +
            ", replySequence=" + replySequence +
            ", instanceId=" + instanceId +
            ", acceptorId=" + acceptorId +
            "} ";
    }

}
