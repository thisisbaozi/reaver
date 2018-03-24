package io.messaginglabs.reaver.com.msg;

public class ProposeReply extends Message {

    private int proposerId;
    private int sequence;
    private long nodeId;
    private long instanceId;
    private long acceptorId;
    private Operation op;

    public int getProposerId() {
        return proposerId;
    }

    public void setProposerId(int proposerId) {
        this.proposerId = proposerId;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(long instanceId) {
        this.instanceId = instanceId;
    }

    public long getAcceptorId() {
        return acceptorId;
    }

    public void setAcceptorId(long acceptorId) {
        this.acceptorId = acceptorId;
    }

    public Operation getOp() {
        return op;
    }

    public void setOp(Operation op) {
        this.op = op;
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

    @Override
    public Operation op() {
        return op;
    }

}
