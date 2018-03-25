package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.core.Value;
import io.messaginglabs.reaver.group.PaxosGroup;
import io.messaginglabs.reaver.utils.ContainerUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaxosGroupConfigs implements GroupConfigs {

    /*
     * Reserves last n versions config
     */
    private static final int RESERVE_NUM = 2;

    private static final Logger logger = LoggerFactory.getLogger(PaxosGroupConfigs.class);

    private final PaxosGroup group;
    private ConfigStorage storage;

    /*
     * configs are still usable, the element at 0 is oldest config, size - 1
     * is the newest config.
     */
    private List<PaxosConfig> configs = new ArrayList<>();

    public PaxosGroupConfigs(PaxosGroup group) {
        this(group, null);
    }

    public PaxosGroupConfigs(PaxosGroup group, ConfigStorage storage) {
        this.group = Objects.requireNonNull(group, "group");

        // do nothing when configs is changed if storage is null
        this.storage = storage;
    }

    private void isApplicable(Value value) {
        Objects.requireNonNull(value, "value");

        if (!value.isChosen()) {
            /*
             * can't apply a not chosen value
             */
            throw new IllegalArgumentException(
                String.format("value(%s) is not chosen", value.toString())
            );
        }

        ByteBuf data = value.getData();
        if (data == null || data.readableBytes() == 0) {
            throw new IllegalArgumentException(
                String.format("void data in value(%s)", value.toString())
            );
        }
    }

    @Override
    public Node add(Value value) {
        isApplicable(value);

        return null;
    }

    @Override
    public Node remove(Value value) {
        isApplicable(value);
        return null;
    }

    private boolean isUsable(long instanceId, Config config) {
        Objects.requireNonNull(config, "config");

        if (instanceId <= 0) {
            // eh...
            return true;
        }

        /*
         * instances which's id is greater than the begin instance of
         * this config are able to run with this config.
         */
        return instanceId >= config.beginInstanceId();
    }

    @Override
    public List<Config> clear(long instanceId) {
        int size = configs.size();
        int reserve = RESERVE_NUM;

        if (size == 0 || size <= reserve) {
            if (logger.isTraceEnabled()) {
                logger.trace("no config of group({}) need to remove, reserve({}), configs({})", group.id(), reserve, size);
            }

            return Collections.emptyList();
        }

        int i;
        int scan = size - reserve;
        for (i = scan; i >= 0; i--) {
            if (isUsable(instanceId, configs.get(i))) {
                break;
            }
        }

        List<Config> useless = null;
        for (int j = 0; j < i; j++) {
            if (useless == null) {
                useless = new ArrayList<>();
            }

            useless.add(configs.remove(j));
        }

        if (useless != null) {
            if (logger.isInfoEnabled()) {
                logger.info("remove some configs({}) of group({}), reserves({})", ContainerUtils.toString(useless, "configs"), group.id(), configs.size());
            }

            sync();
            return useless;
        }

        return Collections.emptyList();
    }

    private void sync() {
        if (storage == null) {
            return ;
        }

        int groupId = group.id();
        try {
            storage.write(groupId, configs);
        } catch (Exception e) {
            String msg = String.format(
                "can't write configs(%s) of group(%d) to config storage",
                ContainerUtils.toString(configs, "configs"),
                groupId
            );

            if (logger.isErrorEnabled()) {
                logger.error(msg, e);
            }

            // freeze group
            group.freeze(msg);
        }
    }

    @Override
    public Config find(long sequence) {
        if (sequence < 0) {
            throw new IllegalArgumentException("invalid sequence: " + sequence);
        }
        return null;
    }
}
