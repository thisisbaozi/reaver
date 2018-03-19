package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.msg.Message;

public interface GroupConfigs {

    void apply(Message.Join msg);
    Config find(long sequence);

}
