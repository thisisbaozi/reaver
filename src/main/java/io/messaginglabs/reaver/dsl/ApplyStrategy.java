package io.messaginglabs.reaver.dsl;

public enum ApplyStrategy {

    /*
     * there's only one thread is response for applying chosen values
     * to state machine.
     */
    SERIAL,

    /*
     * multiple threads to apply chosen values
     */
    PARALLEL

}
