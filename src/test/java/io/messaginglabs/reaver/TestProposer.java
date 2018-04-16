package io.messaginglabs.reaver;

import io.messaginglabs.reaver.com.AbstractServerConnector;
import io.messaginglabs.reaver.com.LocalServer;
import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.ServerConnector;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.config.PaxosGroupConfigs;
import io.messaginglabs.reaver.core.ConcurrentProposer;
import io.messaginglabs.reaver.core.Proposer;
import io.messaginglabs.reaver.utils.AddressUtils;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;

public class TestProposer {

    private final int groupId = 1;
    private String address;

    @Before
    public void init() throws Exception {
        address = AddressUtils.resolveIpV4().getHostAddress();
    }

    @Test
    public void batch() throws Exception {
        int port = 9001;
        long localId = new Node(address, port).id();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Server server = new LocalServer(localId, address, new Consumer<Message>() {
            @Override public void accept(Message message) {

            }
        });
        ServerConnector connector = new AbstractServerConnector(1) {
            @Override
            protected Server newServer(String ip, int port) {
                return null;
            }
        };
        PaxosGroupConfigs configs = new PaxosGroupConfigs(groupId, server, connector, null);

        Proposer proposer = new ConcurrentProposer(groupId, localId, configs, executor);
    }
}
