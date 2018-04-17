package io.messaginglabs.reaver;

import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.core.Ballot;
import io.messaginglabs.reaver.core.BallotsCounter;
import io.messaginglabs.reaver.utils.Crc32;
import io.messaginglabs.reaver.utils.NodeUtils;
import io.messaginglabs.reaver.utils.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;

public class TestUtils {

    @Test
    public void testVoterCounter() throws Exception {
        BallotsCounter counter = new BallotsCounter();
        Assert.assertEquals(counter.nodesPromised(), 0);

        String ip = "128.0.0.1";
        Node node0 = new Node(ip, 5000);
        Node node1 = new Node(ip, 5001);
        Node node2 = new Node(ip, 5002);
        Node node3 = new Node(ip, 5003);

        counter.countPromised(node0.id());
        Assert.assertEquals(counter.nodesPromised(), 1);
        Assert.assertEquals(counter.nodesRejected(), 0);

        System.out.println(counter.dumpAccepted());
        System.out.println(counter.dumpRejected());

        counter.countPromised(node1.id());

        Assert.assertEquals(counter.nodesPromised(), 2);
        Assert.assertEquals(counter.nodesRejected(), 0);

        System.out.println(counter.dumpAccepted());

        counter.countRejected(node2.id());
        Assert.assertEquals(counter.nodesPromised(), 2);
        Assert.assertEquals(counter.nodesRejected(), 1);
        System.out.println(counter.dumpAccepted());
        System.out.println(counter.dumpRejected());

        counter.countPromised(node3.id());
        Assert.assertEquals(counter.nodesPromised(), 3);
        Assert.assertEquals(counter.nodesRejected(), 1);
        System.out.println(counter.dumpAccepted());
        System.out.println(counter.dumpRejected());

        counter.reset();
        Assert.assertEquals(counter.nodesPromised(), 0);
        Assert.assertEquals(counter.nodesRejected(), 0);
        Assert.assertEquals(counter.nodesAnswered(), 0);

        counter.countPromised(node0.id());
        counter.countPromised(node0.id());
        Assert.assertEquals(counter.nodesPromised(), 1);
        Assert.assertEquals(counter.nodesRejected(), 0);

        long nodeId = counter.at(0);
        Assert.assertEquals(nodeId, node0.id());

        counter.countPromised(node1.id());
        nodeId = counter.at(1);
        Assert.assertEquals(nodeId, node1.id());

        try {
            counter.at(2);
            Assert.fail();
        } catch (ArrayIndexOutOfBoundsException cause) {
            // ignore
        }
    }

    @Test
    public void testBallotCompare() throws Exception {
        String ip = "127.0.0.1";
        long nodeId = NodeUtils.unsignedId(ip, 4001);

        Ballot ballot1 = new Ballot(1, nodeId);
        Ballot.CompareResult result = ballot1.compare(0, nodeId);
        Assert.assertTrue(result.isGreater());
        result = ballot1.compare(1, nodeId);
        Assert.assertTrue(result.isEquals());
        result = ballot1.compare(2, nodeId);
        Assert.assertTrue(result.isSmaller());
    }

    @Test
    public void testChecksum() throws Exception {
        String str = "Native method support for java.util.zip.CRC32";
        byte[] bytes = Strings.UTF8Bytes(str);

        ByteBuffer buf0 = MockUtils.wrap();
        int checksum = Crc32.get(buf0);
        ByteBuffer buf1 = ByteBuffer.allocateDirect(bytes.length);
        buf1.put(bytes);
        buf1.flip();
        int checksum1 = Crc32.get(buf1);
        Assert.assertEquals(checksum, checksum1);

        ByteBuf buf2 = ByteBufAllocator.DEFAULT.buffer(bytes.length);
        buf2.writeBytes(bytes);
        int checksum2 = Crc32.get(buf2);
        Assert.assertEquals(checksum, checksum2);

        ByteBuf buf3 = ByteBufAllocator.DEFAULT.directBuffer(bytes.length);
        buf3.writeBytes(bytes);
        int checksum3 = Crc32.get(buf3);
        Assert.assertEquals(checksum, checksum3);
    }
}
