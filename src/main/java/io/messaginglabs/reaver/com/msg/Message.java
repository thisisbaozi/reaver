package io.messaginglabs.reaver.com.msg;

import io.messaginglabs.reaver.core.Opcode;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public abstract class Message {

    public enum Type {
        NORMAL(1),
        EMPTY_OP(2),
        MULTI_EMPTY_OP(3)

        ;
        public final int value;

        Type(int value) {
            this.value = value;
        }
    }

    private static Type match(int value) {
        for (Type type : Type.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }

    private int groupId;
    private Opcode op;

    public void setOp(Opcode op) {
        this.op = op;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getGroupId() {
        return groupId;
    }

    public Opcode op() {
        return op;
    }

    public Type type() {
        /*
         * by default, it's a normal one, the EMPTY_OP and MULTI_EMPTY_OP are
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

    public final ByteBuf encode(ByteBuf buf) {
        Objects.requireNonNull(buf, "buf");

        if (buf.isReadOnly()) {
            throw new IllegalArgumentException("read-only buf");
        }

        if (op() == null) {
            throw new IllegalStateException("unknown opcode msg");
        }

        if (type() == null) {
            throw new IllegalStateException("unknown type msg");
        }

        /*
         * message header:
         *
         * +---------------+-----------------+-------------------+
         * | type(2 bytes) | opcode(2 bytes) | group id(4 bytes) |
         * +---------------+-----------------+-------------------+
         *
         * the magic is used to check whether a message is valid or not.
         */
        buf.writeShort(type().value);
        buf.writeShort(op.value);
        buf.writeInt(groupId);

        encodeBody(buf);
        return buf;
    }

    public final void decode(ByteBuf buf) {
        Objects.requireNonNull(buf, "buf");

        if (buf.readableBytes() < 8) {
            throw new IllegalArgumentException(
                String.format("incomplete msg(%d)", buf.readableBytes())
            );
        }

        int rawType = buf.readShort();
        Type type = match(rawType);
        if (type == null) {
            throw new IllegalStateException("unknown raw message type: " + rawType);
        }

        int rawOp = buf.readShort();
        Opcode op = Opcode.match(rawOp);
        if (op == null) {
            throw new IllegalStateException("unknown raw message opcode: " + rawOp);
        }

        this.op = op;
        decodeBody(buf);
    }

    protected void decodeBody(ByteBuf buf) {
        // empty body
    }

    protected void encodeBody(ByteBuf buf) {
        // empty body
    }

    protected abstract int bodySize();

    public int size() {
        return 8 + bodySize();
    }

}
