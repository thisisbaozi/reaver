package io.messaginglabs.reaver.core;

public interface InstanceCache {

    PaxosInstance get(long id);
    PaxosInstance cache(long id, PaxosInstance instance);

    PaxosInstance erase(long id);

    PaxosInstance createIfAbsent(long id);

    int size();

}
