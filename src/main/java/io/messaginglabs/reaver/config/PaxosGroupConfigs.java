package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.LocalServer;
import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import io.messaginglabs.reaver.utils.ContainerUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaxosGroupConfigs implements GroupConfigs {

    private static final Logger logger = LoggerFactory.getLogger(PaxosGroupConfigs.class);

    /*
     * Reserves last n versions config
     */
    private static final int RESERVE_NUM = 2;

    private final InternalPaxosGroup group;
    private final MetadataStorage storage;
    private final LocalServer localServer;

    /*
     * configs are still usable, the element at 0 is oldest config, size - 1
     * is the current config.
     */
    private List<PaxosConfig> configs = new ArrayList<>();

    public PaxosGroupConfigs(InternalPaxosGroup group) {
        this(group, null);
    }

    public PaxosGroupConfigs(InternalPaxosGroup group, MetadataStorage storage) {
        this.group = Objects.requireNonNull(group, "group");

        // do nothing when configs is changed if storage is null
        this.storage = storage;
        this.localServer = new LocalServer(group);
        initConfigs();
    }

    private void initConfigs() {
        if (storage != null) {
            configs.addAll(storage.fetch(group.id()));

            /*
             * sorts configs order by begin instance id
             */
            configs.sort((o1, o2) -> {
                if (o1.begin() == o2.begin()) {
                    // a bug?
                    throw new IllegalStateException(
                        String.format("two configs(first=%s, second=%s) have same begin id(%d)", o1.toString(), o2.toString(), o1.begin())
                    );
                }

                return Long.compare(o1.begin(), o2.begin());
            });

            if (logger.isInfoEnabled()) {
                logger.info("init configs({}) of group({}) from storage", ContainerUtils.toString(configs, "configs"), group.id());
            }
        }
    }

    @Override
    public int add(PaxosConfig config) {
        Objects.requireNonNull(config, "config");

        logger.info(
            "add new config to group({}), new config({}), active configs({})",
            group.id(),
            config.toString(),
            configs.size()
        );

        configs.add(config);
        sync();

        return configs.size();
    }

    @Override
    public ImmutablePaxosConfig build(long instanceId, long beginId, List<Member> members) {
        Objects.requireNonNull(members, "members");

        if (members.isEmpty()) {
            throw new IllegalArgumentException("can't build a empty config(no members)");
        }

        if (instanceId > beginId) {
            throw new IllegalArgumentException(
                String.format("reconfigure id(%d) should be smaller than begin id(%d)", instanceId, beginId)
            );
        }

        // filter duplicated nods
        List<Member> copy = new ArrayList<>();
        for (Member member : members) {
            if (!copy.contains(member)) {
                copy.add(member);
            }
        }

        // connect with each member
        int idx = 0;
        Server[] servers = new Server[copy.size()];
        for (Node member : copy) {
            Server server;
            if (member.id() == group.local().id()) {
                server = localServer;
            } else {
                server = group.env().connector.connect(member.getIp(), member.getPort());
            }

            if (server == null) {
                throw new IllegalStateException("can't connect with current: " + member.toString());
            }

            servers[idx] = server;
            idx++;
        }


        return new ImmutablePaxosConfig(group.id(), instanceId, beginId, copy.toArray(new Member[copy.size()]), servers);
    }

    private boolean isUsable(long instanceId, PaxosConfig config) {
        Objects.requireNonNull(config, "config");

        if (instanceId <= 0) {
            // eh...
            return true;
        }

        /*
         * instances which's id is greater than the begin instance of
         * this config are able to run with this config.
         */
        return instanceId >= config.begin();
    }

    @Override
    public List<PaxosConfig> clear(long instanceId) {
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

        List<PaxosConfig> useless = null;
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
    public PaxosConfig match(long instanceId) {
        int size = configs.size();
        for (int i = size - 1; i >= 0; i--) {
            PaxosConfig config = configs.get(i);
            if (config == null) {
                throw new NullPointerException("null config in configs");
            }

            if (isUsable(instanceId, config)) {
                return config;
            }
        }

        return null;
    }

    @Override
    public void disconnectServers() {
        for (PaxosConfig config : configs) {
            for (Server server : config.servers()) {
                server.release();
            }
        }
    }

    @Override
    public PaxosConfig current() {
        return configs.isEmpty() ? null : configs.get(0);
    }

    @Override
    public int size() {
        return configs.size();
    }
}
