package io.messaginglabs.jpaxos.core;

public interface Sequencer {

    void set(long sequence);

    long next();

}
