package io.messaginglabs.reaver.debug;

public interface TimePoints extends Iterable<Long> {

    default void trace(String stage) {
        trace(stage, System.currentTimeMillis());
    }

    void trace(String stage, long time);

    long get(String stage);

    /**
     * Returns the duration of time point(stage0) - time point(stage1)
     */
    long duration(String stage0, String stage1);

    /**
     * Removes all of the time points from this set
     */
    void clear();

    /**
     * Returns the number of time points traced
     */
    int size();

}
