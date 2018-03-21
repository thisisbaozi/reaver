package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.utils.Parameters;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;
import java.util.ArrayList;
import java.util.List;

public class DefaultInstanceCache implements InstanceCache {

    private final LongObjectMap<PaxosInstance> map;
    private final List<RecyableInstance> cache;

    /*
     * the maximum number of instance object the cache should maintain
     */
    private final int objectsCapacity;
    private int objectsUsed;

    public DefaultInstanceCache(int capacity, int objectsCapacity) {
        this.cache = new ArrayList<>();
        this.map = new LongObjectHashMap<>(capacity);

        this.objectsUsed = 0;
        this.objectsCapacity = Parameters.requireNotNegativeOrZero(objectsCapacity, "objectsCapacity");
    }

    @Override
    public PaxosInstance newInstance(long id) {
        PaxosInstance instance;

        if (cache.isEmpty()) {
            if (objectsUsed >= objectsCapacity) {
                instance = new PaxosInstance();
            } else {
                instance = new RecyableInstance();
            }
        } else {
            instance = cache.remove(0);
        }

        if (instance == null) {
            throw new IllegalStateException("buggy, no instance");
        }

        instance.reset(id);
        return instance;
    }

    @Override
    public PaxosInstance get(long id) {
        return map.get(id);
    }

    @Override
    public PaxosInstance cache(long id, PaxosInstance instance) {
        return map.put(id, instance);
    }

    @Override
    public PaxosInstance erase(long id) {
        return map.remove(id);
    }

    @Override
    public int size() {
        return map.size();
    }

    private final class RecyableInstance extends PaxosInstance {

        @Override
        protected void deallocate() {
            // reset for reusing this object
            groupId = 0;
            id = 0;
            chosen = null;
            accepted = null;
            proposed = null;

            cache.add(this);
        }

    }
}
