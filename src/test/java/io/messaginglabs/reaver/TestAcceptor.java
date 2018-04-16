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
import org.junit.Test;

public class TestAcceptor {

    @Test
    public void testProcessProposals() throws Exception {
        long instanceId = 1;
        long id0 = MockUtils.localNodeId(9001);
        long id1 = MockUtils.localNodeId(9002);
        long id2 = MockUtils.localNodeId(9003);
        Assert.assertTrue(id1 > id0 && id2 > id1);

        InstanceCache cache = new DefaultInstanceCache(1024, 32);
        ConcurrentAcceptor acceptor = new ConcurrentAcceptor(0, id0, cache);

        Proposing proposing = new Proposing();
        proposing.setInstanceId(instanceId);
        proposing.setType(Proposing.Type.NORMAL);
        proposing.setOp(Opcode.PREPARE);

        proposing.setNodeId(id0);
        proposing.setSequence(4);

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
        ByteBuf value = MockUtils.makeValue("0");
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


}
