package io.messaginglabs.reaver.config;

import java.util.List;

public interface GroupConfigs {

    void initConfig(List<Node> members);

    List<Config> clear(long instanceId);

    Config match(long instanceId);

}
