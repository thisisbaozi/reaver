package io.messaginglabs.reaver;

import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Proposing;
import io.messaginglabs.reaver.core.ConcurrentAcceptor;
import io.messaginglabs.reaver.core.DefaultInstanceCache;
import io.messaginglabs.reaver.core.InstanceCache;
import io.messaginglabs.reaver.core.Opcode;
import io.messaginglabs.reaver.core.PaxosInstance;
import io.netty.buffer.ByteBuf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestAcceptor {

    private long id0;
    private long id1;
    private long id2;
    private ConcurrentAcceptor acceptor;

    @Before
    public void init() throws Exception {
        id0 = MockUtils.localNodeId(9001);
        id1 = MockUtils.localNodeId(9002);
        id2 = MockUtils.localNodeId(9003);

        Assert.assertTrue(id1 > id0 && id2 > id1);

        InstanceCache cache = new DefaultInstanceCache(1024, 32);
        acceptor = new ConcurrentAcceptor(0, id0, cache);
    }

    private Proposing newPrepareProposing(long instanceId, long nodeId, int sequence) {
        Proposing proposing = new Proposing();
        proposing.setInstanceId(instanceId);
        proposing.setType(Proposing.Type.NORMAL);
        proposing.setOp(Opcode.PREPARE);

        proposing.setNodeId(nodeId);
        proposing.setSequence(sequence);

        return proposing;
    }

    @Test
    public void testProcessProposals() throws Exception {
        long instanceId = 1;

        Proposing proposing = newPrepareProposing(instanceId, id0, 4);

        AcceptorReply reply = acceptor.process(proposing);
        Assert.assertEquals(reply.getOp(), Opcode.PREPARE_EMPTY_REPLY);

        proposing.setNodeId(id1);
        reply = acceptor.process(proposing);
        Assert.assertEquals(reply.getOp(), Opcode.PREPARE_EMPTY_REPLY);

        PaxosInstance instance = acceptor.get(instanceId);
        Assert.assertEquals(instance.acceptor().getNodeId(), id1);
        Assert.assertEquals(instance.acceptor().getSequence(), 4);

        proposing.setNodeId(id0);
        reply = acceptor.process(proposing);
        Assert.assertEquals(reply.getOp(), Opcode.REJECT_PREPARE);
        Assert.assertEquals(reply.getNodeId(), id1);
        Assert.assertEquals(reply.getSequence(), 4);

        proposing.setSequence(5);
        reply = acceptor.process(proposing);
        Assert.assertEquals(reply.getOp(), Opcode.PREPARE_EMPTY_REPLY);

        proposing.setOp(Opcode.ACCEPT);
        ByteBuf value = MockUtils.wrap("0");
        proposing.setValue(value);
        reply = acceptor.process(proposing);
        Assert.assertEquals(reply.getOp(), Opcode.ACCEPT_REPLY);

        proposing.setOp(Opcode.PREPARE);
        proposing.setValue(null);
        proposing.setSequence(6);
        reply = acceptor.process(proposing);
        Assert.assertEquals(reply.getOp(), Opcode.PREPARE_REPLY);
        ByteBuf currentValue = reply.getValue();
        Assert.assertEquals(currentValue, value);

        proposing.setOp(Opcode.ACCEPT);
        proposing.setValue(value);
        reply = acceptor.process(proposing);
        Assert.assertEquals(reply.getOp(), Opcode.ACCEPT_REPLY);
    }

    @Test
    public void testProcessMultiValues() throws Exception {
        Proposing proposing = newPrepareProposing(1, id0, 2);

        AcceptorReply reply = acceptor.process(proposing);
        Assert.assertEquals(reply.getOp(), Opcode.PREPARE_EMPTY_REPLY);

        proposing = newPrepareProposing(2, id2, 3);
        reply = acceptor.process(proposing);
        Assert.assertEquals(reply.getOp(), Opcode.PREPARE_EMPTY_REPLY);

        Assert.assertEquals(acceptor.get(1).acceptor().getNodeId(), id0);
        Assert.assertEquals(acceptor.get(2).acceptor().getNodeId(), id2);
    }

    @Test
    public void testProposeChosenValues() throws Exception {

    }

}
