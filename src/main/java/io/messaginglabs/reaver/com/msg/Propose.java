package io.messaginglabs.reaver.com.msg;

import io.netty.buffer.ByteBuf;

public class Propose extends Message {

    public enum Type {
        NORMAL(1),
        EMPTY_OP(2),
        MULTI_EMPTY_OP(3)

        ;
        public final int value;

        Type(int value) {
            this.value = value;
        }

        public boolean isEmpty() {
            return this == EMPTY_OP;
        }

        private static Type match(int value) {
            for (Type type : Type.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return null;
        }
    }

    private int proposerId;
    private int sequence;
    private long nodeId;
    private long instanceId;

    /*
     * combined myValue
     */
    private ByteBuf value;

    /*
     * by default, it's a normal one, the EMPTY_OP and MULTI_EMPTY_OP are
     * used to add padding instances so that a new config can take
     * effect ASAP.
     */
    private Type type = Type.NORMAL;

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

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
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
