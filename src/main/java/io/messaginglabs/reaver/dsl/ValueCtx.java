package io.messaginglabs.reaver.dsl;

import io.netty.buffer.ByteBuf;

public interface ValueCtx {

    /**
     * Returns chosen myValue. As an optimization, the returned myValue may
     * change after this calling finished, the state machine shouldn't
     * rely on the value.
     *
     * You might want to batch a number of values and process them in bulk, deep
     * copy is a choice.
     */
    ByteBuf value();

    /**
     * Returns the attachment specified while learn the value, it could be null.
     */
    Object attachment();

}
