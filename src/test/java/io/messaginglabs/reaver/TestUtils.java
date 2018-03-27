package io.messaginglabs.reaver;

import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.core.VotersCounter;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import org.junit.Assert;
import org.junit.Test;

public class TestUtils {

    @Test
    public void testVoterCounter() throws Exception {
        VotersCounter counter = new VotersCounter();
        Assert.assertEquals(counter.nodesPromised(), 0);

        String ip = "128.0.0.1";
        Node node0 = new Node(ip, 5000);
        Node node1 = new Node(ip, 5001);
        Node node2 = new Node(ip, 5002);
        Node node3 = new Node(ip, 5003);

        counter.countPromised(node0.id());
        Assert.assertEquals(counter.nodesPromised(), 1);
        Assert.assertEquals(counter.nodesRejected(), 0);

        System.out.println(counter.dumpPromised());
        System.out.println(counter.dumpRejected());

        counter.countPromised(node1.id());

        Assert.assertEquals(counter.nodesPromised(), 2);
        Assert.assertEquals(counter.nodesRejected(), 0);

        System.out.println(counter.dumpPromised());

        counter.countRejected(node2.id());
        Assert.assertEquals(counter.nodesPromised(), 2);
        Assert.assertEquals(counter.nodesRejected(), 1);
        System.out.println(counter.dumpPromised());
        System.out.println(counter.dumpRejected());

        counter.countPromised(node3.id());
        Assert.assertEquals(counter.nodesPromised(), 3);
        Assert.assertEquals(counter.nodesRejected(), 1);
        System.out.println(counter.dumpPromised());
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

    }

}
