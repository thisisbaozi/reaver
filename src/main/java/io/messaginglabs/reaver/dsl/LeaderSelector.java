package io.messaginglabs.reaver.dsl;

import java.util.List;

public interface LeaderSelector {

    GroupMember select(List<GroupMember> members);
    
}
