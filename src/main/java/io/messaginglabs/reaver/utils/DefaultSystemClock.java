package io.messaginglabs.reaver.utils;

public class DefaultSystemClock implements SystemClock {

    public long nano() {
        return System.nanoTime();
    }

    public long milliseconds() {
        return System.currentTimeMillis();
    }
}
