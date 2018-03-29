package io.messaginglabs.reaver.dsl;

import java.util.List;

public interface LeaderSelector {

    /**
     * Invoked when the lease of previous leader is expired and it doesn't renew
     * its lease.
     */
    PaxosMember select(List<PaxosMember> members);
    
}
