package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.CommitValue;

public interface Learner {

    /**
     * Returns the max instance id this learner has learned
     *
     * instances in progress:
     *
     * +---------+--------------------+-------------+-----+-------+
     * | n(done) | n + 1(in progress) | n + 2(done) | ... | n + m |
     * +---------+--------------------+-------------+-----+-------+
     *
     * Returns n for above case.
     */
    long instanceId();

    void learn(CommitValue value);

}
