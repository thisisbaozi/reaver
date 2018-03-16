package io.messaginglabs.jpaxos.utils;

import java.util.Objects;

public final class Parameters {

    public static String checkNotEmpty(String value, String message) {
        Objects.requireNonNull(value, message);

        if (value.isEmpty()) {
            throw new IllegalArgumentException(message + " can't be null");
        }

        return value;
    }

}
