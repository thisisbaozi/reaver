package io.messaginglabs.reaver.com.msg;

import io.messaginglabs.reaver.core.Opcode;

public class RemoveNode extends Message {
    @Override
    public Opcode op() {
        return Opcode.REMOVE_NODE;
    }
}
