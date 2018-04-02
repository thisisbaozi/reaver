package io.messaginglabs.reaver.core;

import io.netty.buffer.ByteBuf;
import java.util.Objects;

public final class ValueUtils {

    public static ValueType parse(ByteBuf value) {
        Objects.requireNonNull(value, "value");

        int readable = value.readableBytes();
        if (readable < 4) {
            throw new IllegalArgumentException(String.format("void value, readable bytes %d", readable));
        }

        int rawType = value.getInt(value.readerIndex());
        return ValueType.match(rawType);
    }



}
