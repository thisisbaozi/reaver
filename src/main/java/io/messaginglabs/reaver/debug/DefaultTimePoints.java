package io.messaginglabs.reaver.debug;

import java.util.Iterator;

public class DefaultTimePoints implements TimePoints {

    @Override public void trace(String stage, long time) {

    }

    @Override public long get(String stage) {
        return 0;
    }

    @Override public long duration(String stage0, String stage1) {
        return 0;
    }

    @Override public void clear() {

    }

    @Override public int size() {
        return 0;
    }

    @Override public Iterator<Long> iterator() {
        return null;
    }
}
