package io.messaginglabs.reaver.utils;

import io.netty.buffer.ByteBuf;
import java.util.Objects;

public final class Parameters {

    public static String requireNotEmpty(String value, String message) {
        Objects.requireNonNull(value, message);

        if (value.isEmpty()) {
            throw new IllegalArgumentException(message + " can't be null");
        }

        return value;
    }

    public static int requireNotNegativeOrZero(int value, String msg) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                String.format("%s can't be negative or 0, but given %d", msg, value)
            );
        }

        return value;
    }

    public static int requireNotNegative(int value, String msg) {
        if (value < 0) {
            throw new IllegalArgumentException(
                String.format("%s can't be negative, but given %d", msg, value)
            );
        }

        return value;
    }

    public static ByteBuf requireNotEmpty(ByteBuf buf) {
        Objects.requireNonNull(buf, "buf");

        if (buf.refCnt() == 0) {
            throw new IllegalArgumentException("released buf");
        }

        if (buf.readableBytes() == 0) {
            throw new IllegalArgumentException("empty buf");
        }

        return buf;
    }

}
