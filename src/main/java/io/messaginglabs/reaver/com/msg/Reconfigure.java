package io.messaginglabs.reaver.com.msg;

import io.messaginglabs.reaver.config.Member;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;

public class Reconfigure extends Message {

    private List<Member> members;

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }

    @Override
    protected void decodeBody(ByteBuf buf) {
        int count = buf.readShort();
        if (count == 0) {
            return ;
        }

        members = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Member member = new Member();
            member.setMinVersion(buf.readByte());
            member.setMaxVersion(buf.readByte());

            long nodeId = buf.readLong();
            member.setIp(AddressUtils.parseIp(nodeId));
            member.setPort(AddressUtils.parsePort(nodeId));

            members.add(member);
        }
    }

    @Override
    protected void encodeBody(ByteBuf buf) {
        /*
         * body:
         *
         * +-------+-------------------------+-----+
         * | count | member(min|max|ip|port) | ... |
         * +-------+-------------------------+-----+
         */
        buf.writeInt(members.size());
        for (Member member : members) {
            buf.writeByte(member.getMinVersion());
            buf.writeByte(member.getMaxVersion());
            buf.writeLong(member.id());
        }
    }
}
