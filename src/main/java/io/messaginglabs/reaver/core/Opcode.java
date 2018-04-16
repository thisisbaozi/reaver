package io.messaginglabs.reaver.core;

public enum Opcode {
    PREPARE(1),
    ACCEPT(2),

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
    COMMIT(12),
    LEARN_CHOSEN_VALUE(13),
    NEED_BOOT(14),
    ;

    public final int value;

    Opcode(int value) {
        this.value = value;
    }

    public boolean isJoinGroup() {
        return this == JOIN_GROUP;
    }

    public boolean isLearnChosenValue() {
        return this == LEARN_CHOSEN_VALUE;
    }

    public boolean isPrepare() {
        return this == PREPARE;
    }

    public boolean isPropose() {
        return this == ACCEPT;
    }

    public boolean isNeedBoot() {
        return this == NEED_BOOT;
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
