package io.messaginglabs.reaver.core;

public interface Sequencer {

    void set(long sequence);

    long get();

    long next();

}
