package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.Message;

public interface Configs {

    void apply(Message.Join msg);
    Config get(long sequence);

}
