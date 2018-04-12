package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.core.Defines;
import io.messaginglabs.reaver.utils.ContainerUtils;
import java.util.Arrays;

public class ImmutablePaxosConfig implements PaxosConfig {

    private int groupId;

    private long instanceId;
    private long beginId;

    /*
     * acceptors in this config, this is immutable
     */
    private Server[] servers;
    private Member[] members;

    public ImmutablePaxosConfig(int groupId, long instanceId, long beginId, Member[] members, Server[] servers) {
        this.groupId = groupId;
        this.instanceId = instanceId;
        this.beginId = beginId;
        this.members = members;
        this.servers = servers;

        // determine which version used to communicate
        int version = Defines.VOID_VERSION;
        for (Member member : members) {
            int memberVer = member.getMinVersion();
            if (memberVer > version) {
                version = memberVer;
            }
        }

        if (version == Defines.VOID_VERSION) {
            throw new IllegalStateException("can't determine version from members: " + Arrays.toString(members));
        }
    }

    @Override
    public long instanceId() {
        return instanceId;
    }

    @Override
    public int majority() {
        return members.length / 2;
    }

    @Override
    public int total() {
        return members.length;
    }

    @Override
    public long begin() {
        return beginId;
    }

    @Override
    public Member[] members() {
        return members;
    }

    @Override
    public Server[] servers() {
        return servers;
    }

    @Override
    public boolean isMember(long nodeId) {
        for (Member member : members) {
            if (member.id() == nodeId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Server find(long nodeId) {
        for (Server server : servers) {
            if (server.nodeId() == nodeId) {
                return server;
            }
        }
        return null;
    }

    @Override
    public void broadcast(Message msg) {
        for (Server server : servers) {
            server.send(msg);
        }
    }

    @Override
    public String toString() {
        return "ImmutablePaxosConfig{" +
            "groupId=" + groupId +
            ", instanceId=" + instanceId +
            ", beginId=" + beginId +
            ", servers=" + Arrays.toString(servers) +
            ", members=" + Arrays.toString(members) +
            '}';
    }

}
