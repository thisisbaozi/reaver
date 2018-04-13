package io.messaginglabs.reaver;

import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TestReconfigure {

    @Test
    public void memberJoin() throws Exception {
        InternalPaxosGroup group0 = MockUtils.newGroup(9001);
        group0.join(null);

        Assert.assertEquals(group0.role(), PaxosGroup.Role.FORMAL);
        Assert.assertEquals(group0.state(), PaxosGroup.State.RUNNING);

        List<Node> members = new ArrayList<>();
        members.add(group0.local());

        InternalPaxosGroup group1 = MockUtils.newGroup(9002);
        group1.join(members);

        Thread.sleep(100000000);

        group0.close(-1);
        group1.close(-1);
    }
}
