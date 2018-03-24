package io.messaginglabs.reaver.com.msg;

public class ProposeReply extends Message {

    private int proposerId;
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

    @Override
    public Operation op() {
        return op;
    }

}
