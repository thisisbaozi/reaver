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

}
