package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.group.GroupEnv;
import io.messaginglabs.reaver.group.PaxosGroup;
import java.util.Objects;

public class PaxosGroupConfigs implements GroupConfigs {

    private final PaxosGroup group;

    public PaxosGroupConfigs(PaxosGroup group) {
        this.group = Objects.requireNonNull(group, "group");
    }

    @Override
    public Config apply(UnifiedBoot boot) {
        Objects.requireNonNull(boot, "boot");

        GroupEnv env = group.env();
        if (env.debug) {

        }

        return null;
    }

    @Override
    public Config apply(AddNode event) {
        return null;
    }

    private void apply(Config newConfig) {
        Objects.requireNonNull(newConfig, "newConfig");


    }

    @Override
    public Config find(long sequence) {
        if (sequence < 0) {
            throw new IllegalArgumentException("invalid sequence: " + sequence);
        }
        return null;
    }
}
