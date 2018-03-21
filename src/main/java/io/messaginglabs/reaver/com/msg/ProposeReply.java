package io.messaginglabs.reaver.com.msg;

public class ProposeReply extends Message {

    private int proposerId;
    private long instanceId;

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

    @Override
    public Operation op() {
        return Operation.ACCEPT_REPLY;
    }

}
