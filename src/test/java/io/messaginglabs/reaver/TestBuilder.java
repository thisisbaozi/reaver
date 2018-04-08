package io.messaginglabs.reaver;

import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.dsl.PaxosBuilder;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.messaginglabs.reaver.group.MultiPaxosBuilder;
import io.messaginglabs.reaver.utils.AddressUtils;
import java.nio.ByteBuffer;
import org.junit.Test;

public class TestBuilder {

    private final int port = 8666;


    @Test
    public void testBuild() throws Exception {
        String ip = AddressUtils.resolveIpV4().getHostAddress();

        PaxosBuilder builder = new MultiPaxosBuilder(1);
        builder.setBatchSize(1024 * 1024 * 128);
        builder.setPipeline(4);
        builder.setDir("/tmp/reaver/");
        builder.setNode(new Node(ip, port));
        builder.setLeaderProposeOnly(false);
        builder.setValueCacheCapacity(1024 * 1024 * 256);
        StateMachine sm = new EmptyStateMachine();
        PaxosGroup group = builder.build(sm);

        group.join(null);

        // commit a value
        ByteBuffer value = MockUtils.makeValue();
        group.commit(value);

        Thread.sleep(10000000);
    }
}
