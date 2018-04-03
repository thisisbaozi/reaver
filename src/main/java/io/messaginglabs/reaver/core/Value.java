package io.messaginglabs.reaver.core;

import io.netty.buffer.ByteBuf;

public interface Value {

    /*
     * the max size of the payload of a value
     */
    int SIZE_BITS = 27;
    int MAX_SIZE = (1 << SIZE_BITS) - 1;   // 127m

    /*
     * the header of a value is composed of:
     *
     * 0. size of data(25 bits, max)
     * 1. type(7 bits, 127 is enough)
     * 2. checksum(bytes)
     */
    int HEADER_SIZE = 8;

    int size();
    int checksum();
    ValueType type();

    ByteBuf payload();

}
