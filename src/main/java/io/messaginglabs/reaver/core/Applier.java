package io.messaginglabs.reaver.core;

import java.util.function.Consumer;

public interface Applier extends AutoCloseable {

    void addExceptionListener(Consumer<Throwable> consumer);

    void start() throws Exception;
    void add(PaxosInstance instance);

}
