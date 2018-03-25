package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.core.Value;
import java.util.List;

public interface GroupConfigs {

    Node add(Value value);
    Node remove(Value value);

    List<Config> clear(long instanceId);

    Config find(long sequence);

}
