package io.messaginglabs.reaver.utils;

import java.util.Collection;
import java.util.Objects;

public final class ContainerUtils {

    private ContainerUtils() {

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
}
