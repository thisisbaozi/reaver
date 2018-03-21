package io.messaginglabs.reaver.utils;

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

}
