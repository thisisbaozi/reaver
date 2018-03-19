package io.messaginglabs.reaver.dsl;

import java.nio.ByteBuffer;

/**
 * A failed commit means the Paxos can't commit the value due to some errors.
 */
public interface Value {

    /**
     * Returns the value committed
     */
    ByteBuffer value();

    /**
     * Returns the attachment specified while commit the value
     */
    Object att();

    /**
     * Returns the time when committed this value.
     */
    long begin();

    /**
     * Returns the time when this value is chosen.
     */
    long chosen();

}
