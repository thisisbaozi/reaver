package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.Message;

public class SerialLearner implements Learner {

    @Override
    public long instanceId() {
        return 0;
    }

    @Override
    public void process(Message msg) {

    }

    @Override
    public void close() throws Exception {

    }
}
