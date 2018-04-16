package io.messaginglabs.reaver.core;

import java.util.concurrent.atomic.AtomicLong;

public class IdSequencer extends AtomicLong implements Sequencer {

    private static final long serialVersionUID = -8419501777390345574L;

    @Override
    public long next() {
        return getAndIncrement();
    }

    @Override
    public String toString() {
        return "IdSequencer{ " + super.toString()  + "}";
    }

}
