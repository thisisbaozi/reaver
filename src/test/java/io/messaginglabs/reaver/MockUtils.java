package io.messaginglabs.reaver;

import io.messaginglabs.reaver.config.Member;
import io.messaginglabs.reaver.core.Defines;
import io.messaginglabs.reaver.utils.Strings;
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

}
