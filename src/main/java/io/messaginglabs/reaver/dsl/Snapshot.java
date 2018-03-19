package io.messaginglabs.reaver.dsl;

import java.util.Iterator;

public interface Snapshot extends Iterator<SnapshotFile> {

    /**
     * Returns the address(format -> ip:port) of the provider provided this snapshot
     */
    String provider();

    /**
     * Returns the number of files in this snapshot.
     */
    int size();

    /**
     * Returns the id of last instance applied to this snapshot.
     */
    long instanceId();

}
