package io.messaginglabs.reaver.dsl;

public interface ChosenValues {

    /**
     * Returns the id of this instance, the id is unique absolutely in a group.
     */
    long instanceId();

    /**
     * Returns the number of chosen values in this batch.
     */
    int size();

    /**
     * Returns the value at the specified position in this value batch.
     */
    ChosenValue get(int idx);

}
