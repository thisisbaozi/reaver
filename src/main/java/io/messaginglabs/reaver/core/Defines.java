package io.messaginglabs.reaver.core;

public final class Defines {

    public static final long VOID_INSTANCE_ID = -1;

    public static boolean isValidInstance(long instanceId) {
        return instanceId == VOID_INSTANCE_ID;
    }

    /*
     * the maximum number of nodes allowed to vote as a acceptor for
     * a group.
     */
    public static final int MAX_ACCEPTORS = 24;

}
