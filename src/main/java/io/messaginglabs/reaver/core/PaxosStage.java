package io.messaginglabs.reaver.core;

public enum PaxosStage {

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

    public boolean isAccept() {
        return this == ACCEPT;
    }
}
