package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.config.Config;

public interface Voter extends Participant {

    Config find(long instanceId);
}
