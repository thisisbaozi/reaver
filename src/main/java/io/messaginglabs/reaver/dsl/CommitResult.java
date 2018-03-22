package io.messaginglabs.reaver.dsl;

public enum CommitResult {

    OK,

    /*
     * in general, it's a internal error(bug)
     */
    UNKNOWN_ERROR,

    PROPOSE_THROTTLE,
    NO_CONFIG,
    FROZEN_GROUP,
    CLOSED_GROUP,

    ;
}
