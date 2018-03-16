package io.messaginglabs.jpaxos.com;

public enum Opcode {

    ADD_NODE(1);

    private final int code;

    Opcode(int idx) {
        this.code = idx;
    }

}
