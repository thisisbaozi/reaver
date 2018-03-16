package io.messaginglabs.reaver.core;

public interface Sequencer {

    void set(long sequence);

    long next();

}
