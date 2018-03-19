package io.messaginglabs.reaver.dsl;

public interface SnapshotBuilder {

    void add(String path, int size);
    void add(String path, int size, long begin, long end);

    /**
     * True means the Paxos library is response to delete files in the snapshot once
     * the snapshot is not useful anymore.
     */
    void autoDelete(boolean enable);

}
