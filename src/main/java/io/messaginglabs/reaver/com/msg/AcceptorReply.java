package io.messaginglabs.reaver.com.msg;

import io.messaginglabs.reaver.core.Opcode;
import io.netty.buffer.ByteBuf;

public class AcceptorReply extends Message {

    private int proposerId;

    // proposal
    private int sequence;
    private long nodeId;
    private ByteBuf value;

    private long instanceId;
    private long acceptorId;

    private Opcode op;

    public Opcode getOp() {
        return op;
    }

    public void setOp(Opcode op) {
        this.op = op;
    }

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

    public long getAcceptorId() {
        return acceptorId;
    }

    public void setAcceptorId(long acceptorId) {
        this.acceptorId = acceptorId;
    }

    @Override
    public Opcode op() {
        return op;
    }
}
