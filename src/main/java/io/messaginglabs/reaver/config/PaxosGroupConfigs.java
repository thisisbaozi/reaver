package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.msg.Message;
import java.util.Objects;

public class PaxosGroupConfigs implements Configs {

    @Override
    public void apply(Message.Join msg) {
        Objects.requireNonNull(msg, "msg");


    }

    @Override
    public Config get(long sequence) {
        if (sequence < 0) {
            throw new IllegalArgumentException("invalid sequence: " + sequence);
        }
        return null;
    }
}
