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

    public static final int VOID_VERSION = 0;
    public static final int MIN_VERSION_SUPPORTED = 1;
    public static final int MAX_VERSION_SUPPORTED = 1;

    public static final int CACHE_MAX_CAPACITY = 32000;

    /* transport */

    /*
     * the max size of a packet, it means the size of value must be smaller
     * than this.
     */
    public static final int PACKET_MAX_SIZE = 32 * 1024 * 1024; // 32m

    public static final int CONNECT_TIMEOUT = 2000;
}
