package io.messaginglabs.reaver.com.msg;

import io.messaginglabs.reaver.core.Opcode;

public abstract class Message {

    public enum Type {
        NORMAL(1),
        NONE(2),
        MULTI_NONE(3)

        ;
        public final int value;

        Type(int value) {
            this.value = value;
        }
    }

    private int groupId;

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getGroupId() {
        return groupId;
    }

    abstract public Opcode op();

    public Type type() {
        /*
         * by default, it's a normal one, the NONE and MULTI_NONE are
         * used to add padding instances so that a new config can take
         * effect ASAP.
         */
        return Type.NORMAL;
    }

    public boolean isPrepareReply() {
        return op() == Opcode.PREPARE_REPLY;
    }

    public boolean isEmptyPrepareReply() {
        return op() == Opcode.PREPARE_EMPTY_REPLY;
    }

    public boolean isAccepted() {
        return op() == Opcode.ACCEPT_REPLY;
    }

    public boolean isAcceptRejected() {
        return op() == Opcode.REJECT_ACCEPT;
    }

}
