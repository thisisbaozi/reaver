package io.messaginglabs.reaver.core;

public final class Defines {

    private static final long VOID_INSTANCE_ID = -1;

    public static boolean isValidInstance(long instanceId) {
        return instanceId == VOID_INSTANCE_ID;
    }

}
