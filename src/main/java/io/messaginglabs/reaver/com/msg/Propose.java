package io.messaginglabs.reaver.com.msg;

import io.netty.buffer.ByteBuf;

public class Propose extends Message {

    /*
     * ProposeContext number
     */
    private int sequence;
    private long nodeId;

    /*
     * paxos instance id
     */
    private long instanceId;

    /*
     * combined value
     */
    private ByteBuf value;
    private Operation op;

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

    public Operation getOp() {
        return op;
    }

    public void setOp(Operation op) {
        this.op = op;
    }

    @Override
    public Operation op() {
        return op;
    }

}
