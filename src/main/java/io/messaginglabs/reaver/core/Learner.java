package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.Message;

public interface Learner extends AutoCloseable {

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

    void process(Message msg);

}
