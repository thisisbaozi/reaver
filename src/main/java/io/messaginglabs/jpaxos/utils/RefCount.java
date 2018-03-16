package io.messaginglabs.jpaxos.utils;

public interface RefCount {

    int refCount();

    int release();

    int retain();

}
