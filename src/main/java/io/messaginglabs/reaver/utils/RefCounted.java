package io.messaginglabs.reaver.utils;

public abstract class RefCounted implements RefCount {

    private int count;

    @Override
    public int refCount() {
        return 0;
    }

    @Override
    public int release() {
        return 0;
    }

    @Override
    public int retain() {
        return 0;
    }

    protected abstract void deallocate();

}
