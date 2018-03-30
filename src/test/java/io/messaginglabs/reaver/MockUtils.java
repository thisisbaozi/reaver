package io.messaginglabs.reaver;

import io.messaginglabs.reaver.config.Member;
import io.messaginglabs.reaver.core.Defines;
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

}
