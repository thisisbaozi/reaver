package io.messaginglabs.reaver.utils;

public interface RefCount {

    int refCount();

    int release();

    int retain();

}
