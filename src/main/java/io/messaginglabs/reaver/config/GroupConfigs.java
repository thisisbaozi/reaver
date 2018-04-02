package io.messaginglabs.reaver.config;

import java.util.List;

public interface GroupConfigs {

    int add(PaxosConfig cfg);
    PaxosConfig build(long instanceId, long begin, List<Member> members);

    List<PaxosConfig> clear(long instanceId);

    PaxosConfig match(long instanceId);

    void disconnectServers();

    PaxosConfig newest();

    int size();
}
