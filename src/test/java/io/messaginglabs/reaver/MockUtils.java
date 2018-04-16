package io.messaginglabs.reaver;

import io.messaginglabs.reaver.config.Member;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.core.Defines;
import io.messaginglabs.reaver.dsl.PaxosBuilder;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import io.messaginglabs.reaver.group.MultiPaxosBuilder;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.messaginglabs.reaver.utils.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.nio.ByteBuffer;
import java.util.List;

class MockUtils {

    static Member newMember(String ip, int port, List<Member> members) {
        Member member = new Member();
        member.setIp(ip);
        member.setPort(port);
        member.setMinVersion(Defines.MIN_VERSION_SUPPORTED);
        member.setMaxVersion(Defines.MAX_VERSION_SUPPORTED);

        members.add(member);
        return member;
    }

    static ByteBuffer makeValue() {
        String str = "Native method support for java.util.zip.CRC32";
        byte[] bytes = Strings.UTF8Bytes(str);
        ByteBuffer buf0 = ByteBuffer.allocate(bytes.length);
        buf0.put(bytes);
        buf0.flip();
        return buf0;
    }

    static ByteBuf makeValue(String str) {
        ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
        byte[] bytes = Strings.UTF8Bytes(str);
        ByteBuf buf = allocator.buffer(bytes.length);
        buf.writeBytes(bytes);
        return buf;
    }

    public static InternalPaxosGroup newGroup(int port) throws Exception {
        String ip = AddressUtils.resolveIpV4().getHostAddress();
        PaxosBuilder builder = new MultiPaxosBuilder(1);
        builder.setBatchSize(1024 * 1024 * 128);
        builder.setPipeline(4);
        builder.setDir("/tmp/reaver/");
        builder.setNode(new Node(ip, port));
        builder.setLeaderProposeOnly(false);
        builder.setValueCacheCapacity(1024 * 1024 * 256);
        StateMachine sm = new EmptyStateMachine();
        return (InternalPaxosGroup)builder.build(sm);
    }


    public static Node local(int port) throws Exception {
        String ip = AddressUtils.resolveIpV4().getHostAddress();
        return new Node(ip, port);
    }

    public static long localNodeId(int port) throws Exception {
        return local(port).id();
    }
}
