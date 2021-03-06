package io.messaginglabs.reaver.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class ContainerUtils {

    private ContainerUtils() {

    }

    public static <T> String toString(T[] elements, String name) {
        return toString(Arrays.asList(elements), name);
    }

    public static <T> String toString(Collection<T> elements, String name) {
        Objects.requireNonNull(elements, "elements");

        if (elements.isEmpty()) {
            return name + "()";
        }

        int size = elements.size();
        int num = 0;
        StringBuilder builder = new StringBuilder(name);
        builder.append("(");

        for (T element : elements) {
            if (element == null) {
                continue;
            }

            builder.append(element.toString());
            num++;

            if (size < num - 1) {
                builder.append(",");
            }
        }

        return builder.toString();
    }

    public static void checkNotEmpty(Collection<?> src, String msg) {
        Objects.requireNonNull(src, msg);
        if (msg.isEmpty()) {
            throw new IllegalArgumentException(String.format("%s can't be empty", msg));
        }
    }

    public static <T> List<T> toList(T element) {
        List<T> elements = new ArrayList<>();
        elements.add(element);
        return elements;
    }

}
