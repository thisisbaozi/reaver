package io.messaginglabs.reaver.core;

import io.netty.buffer.ByteBuf;

public class Value {

    /*
     * the max size of the payload of a myValue
     */
    static final int SIZE_BITS = 27;
    public static final int MAX_SIZE = (1 << SIZE_BITS) - 1;   // 127m

    /*
     * the header of a myValue is composed of:
     *
     * 0. size of data(25 bits, max)
     * 1. getType(7 bits, 127 is enough)
     * 2. checksum(bytes)
     */
    public static final int HEADER_SIZE = 8;

    private ByteBuf value;

    public void reset(ByteBuf value) {
        this.value = value;
    }

    public int size() {
        return 0;
    }

    public int checksum() {
        return 0;
    }

    public ValueType type() {
        int header = value.getInt(value.readerIndex());
        return ValueUtils.parse(header);
    }

}
