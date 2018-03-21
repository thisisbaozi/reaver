package io.messaginglabs.reaver.com.msg;

public abstract class Message {

    public enum Operation {
        PREPARE(1),
        PROPOSE(2),

        PREPARE_REPLY(3),
        PREPARE_EMPTY_REPLY(4),
        ACCEPT_REPLY(5),

        // config
        ADD_NODE(6),
        REMOVE_NODE(7),
        FORCE_CONFIG(8),

        // boot
        UNIFIED_BOOT(9)

        ;

        public final int value;

        Operation(int value) {
            this.value = value;
        }
    }

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

    abstract public Operation op();

    public Type type() {
        /*
         * by default, it's a normal one, the NONE and MULTI_NONE are
         * used to add padding instances so that a new config can take
         * effect ASAP.
         */
        return Type.NORMAL;
    }

}
