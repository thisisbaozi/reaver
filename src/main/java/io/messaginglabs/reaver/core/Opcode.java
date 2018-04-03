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
    JOIN_GROUP(8),
    REMOVE_NODE(9),
    FORCE_CONFIG(10),

    // boot
    UNIFIED_BOOT(11),
    LEARN_VALUE(12),
    ;

    public final int value;

    Opcode(int value) {
        this.value = value;
    }

    public boolean isJoinGroup() {
        return this == JOIN_GROUP;
    }

    public boolean isPrepare() {
        return this == PREPARE;
    }

    public boolean isPropose() {
        return this == PROPOSE;
    }

    public static Opcode match(int value) {
        for (Opcode opcode : Opcode.values()) {
            if (opcode.value == value) {
                return opcode;
            }
        }

        return null;
    }
}
