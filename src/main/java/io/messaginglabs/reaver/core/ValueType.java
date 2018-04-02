package io.messaginglabs.reaver.core;

public enum ValueType {

    APP_DATA(0),
    MEMBER_JOIN(1),
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

    public boolean isMemberJoin() {
        return this == MEMBER_JOIN;
    }

}
