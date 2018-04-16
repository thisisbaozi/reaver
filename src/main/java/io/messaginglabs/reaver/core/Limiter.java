package io.messaginglabs.reaver.core;

import java.util.function.Function;

/**
 * Defining a new object instead of using {@link Function} is for reducing Box/Unbox
 * of primitive type(long)
 */
public interface Limiter {

    boolean isEnable(long instanceId);

}
