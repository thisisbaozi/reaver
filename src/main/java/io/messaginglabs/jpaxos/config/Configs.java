package io.messaginglabs.jpaxos.config;

import io.messaginglabs.jpaxos.com.Message;

public interface Configs {

    void apply(Message.Join msg);
    Config get(long sequence);

}
