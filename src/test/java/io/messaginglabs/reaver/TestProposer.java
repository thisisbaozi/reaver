package io.messaginglabs.reaver;

import io.messaginglabs.reaver.com.AbstractServerConnector;
import io.messaginglabs.reaver.com.LocalServer;
import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.ServerConnector;
import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.com.msg.Proposing;
import io.messaginglabs.reaver.config.Member;
import io.messaginglabs.reaver.config.PaxosConfig;
import io.messaginglabs.reaver.config.PaxosGroupConfigs;
import io.messaginglabs.reaver.core.ConcurrentProposer;
import io.messaginglabs.reaver.core.DefaultSerialProposer;
import io.messaginglabs.reaver.core.GenericCommit;
import io.messaginglabs.reaver.core.Opcode;
import io.messaginglabs.reaver.core.PaxosStage;
import io.messaginglabs.reaver.core.ValueType;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.messaginglabs.reaver.utils.NodeUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestProposer {

    private final int groupId = 1;
    private long id0;
    private String address;
    private ConcurrentProposer proposer;
    private PaxosGroupConfigs configs;
    private final int timeout = 1000;

    class MockLocalServer extends LocalServer {
        public final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();

        public MockLocalServer(long localId, String address) {
            super(localId, address, null);
        }

        @Override
        public void send(Message msg) {
            messages.add(msg);
        }
    }

    @Before
    public void init() throws Exception {
        address = AddressUtils.resolveIpV4().getHostAddress();
        id0 = MockUtils.localNodeId(9001);
        ServerConnector connector = new AbstractServerConnector(1) {
            @Override
            protected Server newServer(String ip, int port) {
                return new MockLocalServer(NodeUtils.unsignedId(ip, port), ip + ":" + port);
            }
        };

        configs = new PaxosGroupConfigs(groupId, connector.connect(address, 9001), connector, null);
        List<Member> members = MockUtils.mockMembers(9001, 9002, 9003);
        configs.add(configs.build(1, 1, members));

        proposer = new ConcurrentProposer(groupId, id0, configs, Executors.newSingleThreadScheduledExecutor());
        proposer.setTimeout(timeout);
        proposer.init();
        proposer.sequencer().set(2);
    }

    @Test
    public void batch() throws Exception {
        ByteBuf v0 = MockUtils.createValue("v0", ValueType.APP_DATA);
        ByteBuf v1 = MockUtils.createValue("v1", ValueType.APP_DATA);

        GenericCommit commit0 = proposer.newCommit(v0, null);
        Assert.assertEquals(commit0.valueType(), ValueType.APP_DATA);
        GenericCommit commit1 = proposer.newCommit(v1, null);

        proposer.enqueue(commit0);
        proposer.enqueue(commit1);
        Assert.assertEquals(proposer.getCommits(), 2);

        List<GenericCommit> batch = new ArrayList<>();
        int bytes = proposer.batch(batch);

        Assert.assertEquals(bytes, v0.readableBytes() + v1.readableBytes());
        Assert.assertEquals(batch.size(), 2);
        Assert.assertEquals(proposer.getCommits(), 0);

        // config in commits
        proposer.newCommit(v0, null);
        proposer.newCommit(v1, null);

        ByteBuf v2 = MockUtils.createValue("v2", ValueType.MEMBER_JOIN);
        ByteBuf v3 = MockUtils.createValue("v3", ValueType.APP_DATA);
        commit0 = proposer.newCommit(v0, null);
        commit1 = proposer.newCommit(v1, null);
        GenericCommit c2 = proposer.newCommit(v2, null);
        GenericCommit c3 = proposer.newCommit(v3, null);
        Assert.assertTrue(proposer.enqueue(commit0));
        Assert.assertTrue(proposer.enqueue(commit1));
        Assert.assertTrue(proposer.enqueue(c2));
        Assert.assertTrue(proposer.enqueue(c3));
        Assert.assertEquals(proposer.getCommits(), 4);

        batch.clear();
        proposer.batch(batch);
        Assert.assertEquals(batch.size(), 2);

        batch.clear();

        proposer.batch(batch);
        Assert.assertEquals(batch.size(), 1);
        Assert.assertEquals(batch.get(0).valueType(), ValueType.MEMBER_JOIN);
        Assert.assertEquals(proposer.getCommits(), 1);

        batch.clear();
        proposer.batch(batch);
        Assert.assertEquals(batch.size(), 1);
        Assert.assertEquals(batch.get(0).valueType(), ValueType.APP_DATA);

        // too many commits in buffer
        int count = 128;
        int maxSize = 1024 * 4;
        proposer.setMaxBatchSize(maxSize);

        for (int i = 0; i < count; i++) {
            ByteBuf v = MockUtils.createValue(1024, ValueType.APP_DATA);
            GenericCommit commit = proposer.newCommit(v, null);
            proposer.enqueue(commit);
        }

        Assert.assertEquals(proposer.getCommits(), count);

        batch.clear();
        bytes = proposer.batch(batch);
        Assert.assertTrue(bytes <= maxSize);
    }

    @Test
    public void testSerialPropose() throws Exception {
        DefaultSerialProposer proposer = (DefaultSerialProposer)this.proposer.find();
        Assert.assertFalse(proposer.isBusy());
        proposer.disableTimeoutCheck();

        Assert.assertEquals(proposer.process(), DefaultSerialProposer.Result.NO_VALUE);

        // no config
        ByteBuf v0 = MockUtils.createValue("v0", ValueType.APP_DATA);
        GenericCommit commit0 = this.proposer.newCommit(v0, null);
        this.proposer.enqueue(commit0);
        List<GenericCommit> cache = proposer.valueCache();
        Assert.assertTrue(this.proposer.batch(cache) > 1);
        Assert.assertTrue(proposer.setCommits(cache));
        Assert.assertTrue(proposer.isBusy());
        Assert.assertTrue(proposer.hasValue());
        Assert.assertEquals(proposer.ready(), DefaultSerialProposer.Result.NO_CONFIG);

        // mock a config
        List<Member> members = MockUtils.mockMembers(9001, 9002, 9003);
        PaxosConfig config = configs.build(1, 1, members);
        configs.add(config);
        Assert.assertEquals(configs.match(1), config);

        // propose
        Assert.assertEquals(proposer.ready(), DefaultSerialProposer.Result.READY);
        proposer.accept();

        for (Server server : config.servers()) {
            MockLocalServer mServer = (MockLocalServer)server;
            Assert.assertEquals(mServer.messages.size(), 1);
            mServer.messages.clear();
        }

        Assert.assertTrue(proposer.ctx().stage().isAccept());
        Assert.assertEquals(proposer.process(), DefaultSerialProposer.Result.PROPOSING);
        Assert.assertFalse(proposer.isExpired());

        // timeout
        Thread.sleep(timeout + 20);
        Assert.assertTrue(proposer.isExpired());
        Assert.assertEquals(proposer.retryIfExpired(), DefaultSerialProposer.Result.PROPOSE_AGAIN);

        for (Server server : config.servers()) {
            MockLocalServer mServer = (MockLocalServer)server;
            Assert.assertEquals(mServer.messages.size(), 1);
            Proposing proposing = (Proposing)mServer.messages.poll();
            Assert.assertEquals(proposing.getSequence(), 1);
        }
        Assert.assertFalse(proposer.isExpired());
        Assert.assertEquals(proposer.ctx().stage(), PaxosStage.PREPARE);
    }

    @Test
    public void testRejectPrepare() throws Exception {

    }

    @Test
    public void testRejectAccept() throws Exception {
        this.proposer.enqueue(this.proposer.newCommit(MockUtils.createValue("v0", ValueType.APP_DATA), null));
        DefaultSerialProposer proposer = (DefaultSerialProposer)this.proposer.proposeOneValue();
        proposer.disableTimeoutCheck();
        Assert.assertNotNull(proposer);
        Assert.assertEquals(proposer.ctx().stage(), PaxosStage.ACCEPT);
        for (Server server : configs.match(1).servers()) {
            ((MockLocalServer)server).messages.clear();
        }

        AcceptorReply reply = MockUtils.newReply(
            configs.match(1).members()[0].id(),
            0,
            configs.match(1).members()[0].id(),
            proposer.ctx().instanceId(),
            Opcode.ACCEPT_REPLY
        );

        // pass
        this.proposer.process(reply);
        Assert.assertEquals(proposer.ctx().acceptCounter().nodesPromised(), 1);

        // reject
        reply.setOp(Opcode.REJECT_ACCEPT);
        reply.setNodeId(configs.match(1).members()[1].id());
        reply.setSequence(1);
        reply.setAcceptorId(configs.match(1).members()[1].id());

        Assert.assertTrue(proposer.ctx().stage().isAccept());

        this.proposer.process(reply);
        Assert.assertEquals(proposer.ctx().acceptCounter().nodesRejected(), 1);
        Assert.assertEquals(proposer.ctx().getGreatestSeen().getSequence(), 1);

        // reject
        reply.setNodeId(configs.match(1).members()[2].id());
        reply.setSequence(2);
        reply.setAcceptorId(configs.match(1).members()[2].id());

        this.proposer.process(reply);
        Assert.assertTrue(proposer.ctx().stage().isPrepare());

        for (Server server : configs.match(1).servers()) {
            Opcode op = ((MockLocalServer)server).messages.poll().getOp();
            Assert.assertTrue(op.isPrepare());
        }
    }

    @Test
    public void testPreparePass() throws Exception {
        DefaultSerialProposer proposer = (DefaultSerialProposer)this.proposer.find();
        proposer.disableTimeoutCheck();

        this.proposer.enqueue(this.proposer.newCommit(MockUtils.createValue("v0", ValueType.APP_DATA), null));
        this.proposer.batch(proposer.valueCache());
        proposer.setCommits(proposer.valueCache());
        Assert.assertEquals(proposer.ready(), DefaultSerialProposer.Result.READY);
        proposer.accept();
        long instanceId = proposer.ctx().instanceId();

        for (Server server : configs.match(1).servers()) {
            ((MockLocalServer)server).messages.clear();
        }

        // mock reply
        AcceptorReply reply = MockUtils.newReply(
            configs.match(1).members()[0].id(),
            0,
            configs.match(1).members()[0].id(),
            instanceId,
            Opcode.PREPARE_EMPTY_REPLY
        );

        proposer.process(reply);
        Assert.assertEquals(proposer.ctx().counter().nodesPromised(), 1);

        // duplicate reply
        proposer.process(reply);
        Assert.assertEquals(proposer.ctx().counter().nodesPromised(), 1);

        reply.setAcceptorId(configs.match(1).members()[1].id());
        proposer.process(reply);
        Assert.assertEquals(proposer.ctx().counter().nodesPromised(), 2);

        Assert.assertTrue(proposer.ctx().stage().isAccept());

        for (Server server : configs.match(1).servers()) {
            MockLocalServer mServer = (MockLocalServer)server;
            Assert.assertEquals(mServer.messages.poll().getOp(), Opcode.ACCEPT);
        }
    }
}
