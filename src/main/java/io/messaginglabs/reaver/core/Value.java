package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.utils.Crc32;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.Objects;

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
     * 1. type(7 bits, 127 is enough)
     * 2. checksum(bytes)
     */
    public static final int HEADER_SIZE = 8;

    public static ValueType parse(ByteBuf value) {
        Objects.requireNonNull(value, "value");

        int readable = value.readableBytes();
        if (readable < 8) {
            throw new IllegalArgumentException(String.format("void value, readable bytes %d", readable));
        }

        int header = value.getInt(4);
        return parse(header);
    }

    public static ByteBuf init(ValueType type, ByteBuffer value, ByteBuf buf) {
        Objects.requireNonNull(type, "type");

        // checksum
        int checksum = Crc32.get(value);
        int size = value.remaining();

        buf.writeInt(checksum);
        buf.writeInt(combine(size, type));
        buf.writeBytes(value);

        return buf;
    }

    public static ByteBuf init(ValueType type, ByteBuf value) {
        int size = value.readableBytes() - Value.HEADER_SIZE;

        int readerIdx = value.readerIndex();
        value.readerIndex(readerIdx + Value.HEADER_SIZE);
        int checksum = Crc32.get(value);
        value.readerIndex(readerIdx);

        value.setInt(0, checksum);
        value.setInt(0, combine(size, type));

        return value;
    }

    public static int combine(int size, ValueType type) {
        return (size << (Integer.SIZE - Value.SIZE_BITS)) | type.idx;
    }

    public static ValueType parse(int header) {
        int mask = (1 << (Integer.SIZE - Value.SIZE_BITS)) - 1;
        return ValueType.match(header & mask);
    }

    public static int parseSize(int header) {
        return header >> (Integer.SIZE - Value.SIZE_BITS);
    }


    public ByteBuf appData() {
        return null;
    }
}
