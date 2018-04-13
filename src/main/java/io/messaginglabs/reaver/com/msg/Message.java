package io.messaginglabs.reaver.com.msg;

import io.messaginglabs.reaver.core.Opcode;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public abstract class Message {

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

    public boolean isPropose() {
        return op == Opcode.PROPOSE;
    }

    public boolean isPrepare() {
        return op == Opcode.PREPARE;
    }

    public boolean isRejectPrepare() {
        return op == Opcode.REJECT_PREPARE;
    }

    public boolean isPrepareReply() {
        return op() == Opcode.PREPARE_REPLY;
    }

    public boolean isEmptyPrepareReply() {
        return op() == Opcode.PREPARE_EMPTY_REPLY;
    }

    public boolean isPromiseAcceptProposal() {
        return op() == Opcode.ACCEPT_REPLY;
    }

    public boolean isRefuseAcceptProposal() {
        return op() == Opcode.REJECT_ACCEPT;
    }

    public final ByteBuf encode(ByteBuf src) {
        Objects.requireNonNull(src, "src");

        if (src.isReadOnly()) {
            throw new IllegalArgumentException("read-only src");
        }

        if (op == null) {
            throw new IllegalStateException("unknown opcode msg");
        }

        /*
         * message header:
         *
         * 0. magic code(2 bytes)
         * 1. opcode(2 byte)
         * 2. group id(4 bytes)
         */
        src.writeShort(0);
        src.writeShort(op.value);
        src.writeInt(groupId);

        encodeBody(src);
        return src;
    }

    @SuppressWarnings("all")
    public static <T extends Message> T decode(ByteBuf buf) {
        Objects.requireNonNull(buf, "buf");

        if (buf.readableBytes() < 8) {
            throw new IllegalArgumentException(
                String.format("incomplete msg(%d)", buf.readableBytes())
            );
        }

        int version = buf.readShort();
        int rawOp = buf.readShort();
        Opcode op = Opcode.match(rawOp);
        if (op == null) {
            throw new IllegalStateException("unknown raw message opcode: " + rawOp);
        }

        Message msg;
        if (op.isPropose() || op.isPrepare()) {
            msg = new Propose();
        } else if (op.isJoinGroup()) {
            msg = new Reconfigure();
        }else {
            throw new IllegalStateException("buggy, unknown operation: " + op.name());
        }

        int groupId = buf.readInt();

        msg.setOp(op);
        msg.setGroupId(groupId);
        msg.decodeBody(buf);

        return (T)msg;
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
