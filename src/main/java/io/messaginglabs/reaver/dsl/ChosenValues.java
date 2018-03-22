package io.messaginglabs.reaver.dsl;

import java.util.Iterator;

public interface ChosenValues extends Iterator<ValueCtx> {

    /**
     * Returns the id of this instance, the id is unique absolutely in a group.
     */
    long instanceId();

    /**
     * Returns the number of chosen values in this batch.
     */
    int size();

}
