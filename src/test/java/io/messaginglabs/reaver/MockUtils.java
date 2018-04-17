package io.messaginglabs.reaver;

import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.config.Member;
import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.core.Defines;
import io.messaginglabs.reaver.core.Opcode;
import io.messaginglabs.reaver.core.Value;
import io.messaginglabs.reaver.core.ValueType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    static ByteBuffer wrap() {
        String str = "Native method support for java.util.zip.CRC32";
        return wrapInNioBuffer(str);
    }

    static ByteBuffer wrapInNioBuffer(String str) {
        byte[] bytes = Strings.UTF8Bytes(str);
        ByteBuffer buf0 = ByteBuffer.allocate(bytes.length);
        buf0.put(bytes);
        buf0.flip();
        return buf0;
    }

    static ByteBuf wrap(String str) {
        ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
        byte[] bytes = Strings.UTF8Bytes(str);
        ByteBuf buf = allocator.buffer(bytes.length);
        buf.writeBytes(bytes);
        return buf;
    }

    static ByteBuf createValue(int length, ValueType type) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            int v = ThreadLocalRandom.current().nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            data[i] = (byte)v;
        }

        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data);
        buffer.flip();

        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(buffer.remaining()+ Value.HEADER_SIZE);
        return Value.init(type, buffer, buf);
    }

    static ByteBuf createValue(String data, ValueType type) {
        byte[] bytes = Strings.UTF8Bytes(data);
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(bytes.length + Value.HEADER_SIZE);
        return Value.init(type, wrapInNioBuffer(data), buf);
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


    private static Node local(int port) throws Exception {
        String ip = AddressUtils.resolveIpV4().getHostAddress();
        return new Node(ip, port);
    }

    static long localNodeId(int port) throws Exception {
        return local(port).id();
    }

    static List<Member> mockMembers(int... ports) throws Exception {
        String ip = AddressUtils.resolveIpV4().getHostAddress();
        List<Member> members = new ArrayList<>();
        for (int port : ports) {
            Member member = new Member();
            member.setMinVersion(Defines.MIN_VERSION_SUPPORTED);
            member.setMaxVersion(Defines.MAX_VERSION_SUPPORTED);
            member.setIp(ip);
            member.setPort(port);
            members.add(member);
        }

        return members;
    }

    public static AcceptorReply newReply(long nodeId, int sequence, long acceptorId, long instanceId, Opcode code) {
        AcceptorReply reply = new AcceptorReply();
        reply.setNodeId(nodeId);
        reply.setAcceptorId(acceptorId);
        reply.setSequence(sequence);
        reply.setOp(code);
        reply.setInstanceId(instanceId);

        return reply;
    }

}
