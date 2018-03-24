package io.messaginglabs.reaver.core;

public enum PaxosPhase {

    READY,
    PREPARE,
    ACCEPT,
    COMMIT

    ;

    public boolean isReady() {
        return this == READY;
    }

    public boolean isPrepare() {
        return this == PREPARE;
    }
}
