package io.messaginglabs.reaver.core;

public enum ValueType {

    UNKNOWN(0),
    APP_DATA(1),
    MEMBER_JOIN(2),
    REMOVE_MEMBER(3),
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

    public boolean isRemoveMember() {
        return this == REMOVE_MEMBER;
    }

    public boolean isAppData() {
        return this == APP_DATA;
    }

}
