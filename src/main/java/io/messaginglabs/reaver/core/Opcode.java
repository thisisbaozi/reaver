package io.messaginglabs.reaver.core;

public enum Opcode {
    PREPARE(1),
    PROPOSE(2),

    PREPARE_REPLY(3),
    PREPARE_EMPTY_REPLY(4),
    REJECT_PREPARE(5),
    ACCEPT_REPLY(6),
    REJECT_ACCEPT(7),


    // config
    ADD_NODE(8),
    REMOVE_NODE(9),
    FORCE_CONFIG(10),

    // boot
    UNIFIED_BOOT(11)

    ;

    public final int value;

    Opcode(int value) {
        this.value = value;
    }
}
