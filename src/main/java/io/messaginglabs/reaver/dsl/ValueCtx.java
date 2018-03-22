package io.messaginglabs.reaver.dsl;

import io.netty.buffer.ByteBuf;

/**
 * A failed commit means the Paxos can't commit the value due to some errors.
 */
public interface ValueCtx {

    /**
     * Returns chosen value. As an optimization, the returned value may
     * change after this calling finished, the state machine shouldn't
     * rely on the value.
     *
     * You might want to batch a number of values and process them in bulk, deep
     * copy is a choice.
     */
    ByteBuf get();

    /**
     * Returns the attachment specified while commit the value, it could be null.
     */
    Object attachment();

    /**
     * Returns the time when committed this value.
     */
    long begin();

    /**
     * Returns the time when this value is chosen.
     */
    long chosen();

}
