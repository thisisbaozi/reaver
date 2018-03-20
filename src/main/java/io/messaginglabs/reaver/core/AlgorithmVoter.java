package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.config.Config;
import io.messaginglabs.reaver.config.GroupConfigs;

public abstract class AlgorithmVoter extends AlgorithmParticipant implements Voter {

    @Override
    public Config find(long instanceId) {
        inLoop();

        if (instanceId < 0) {
            throw new IllegalArgumentException("instance must be 0 or positive number, but given: " + instanceId);
        }

        GroupConfigs configs = group().configs();
        if (configs == null) {
            throw new IllegalStateException(
                String.format("no configs in group(%s)", group().id())
            );
        }

        return configs.find(instanceId);
    }
}
