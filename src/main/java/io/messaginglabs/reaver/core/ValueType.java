package io.messaginglabs.reaver.core;

public enum ValueType {

    UNKNOWN(0),
    APP_DATA(1),
    MEMBER_JOIN(2),
    MEMBER_LEAVE(3),

    ;

    public final int idx;

    ValueType(int idx) {
        this.idx = idx;
    }

    public static ValueType match(int idx) {
        ValueType[] types = ValueType.values();
        if (idx >= types.length) {
            return null;
        }

        return types[idx];
    }

    public boolean isJoin() {
        return this == MEMBER_JOIN;
    }

    public boolean isLeave() {
        return this == MEMBER_LEAVE;
    }

    public boolean isAppData() {
        return this == APP_DATA;
    }

}
