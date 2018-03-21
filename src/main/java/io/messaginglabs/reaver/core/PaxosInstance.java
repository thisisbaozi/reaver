package io.messaginglabs.reaver.core;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;

public class PaxosInstance extends AbstractReferenceCounted {

    /*
     * the group this Paxos instance belongs to
     */
    protected int groupId;

    /*
     * unique id in group
     */
    protected long id;

    // this node proposed proposal for this instance
    protected Proposal proposed;

    // chosen proposal
    protected Proposal chosen;
    protected Proposal promised;

    public void reset(long id) {
        this.id = id;
    }

    /**
     * Returns true if the Paxos instance is done
     *
     * DONE means the members in a specified config have reached a consensus
     * on this instance, the value has been chosen.
     */
    public boolean isDone() {
        return chosen != null;
    }

    @Override
    protected void deallocate() {
        // by default, do nothing
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

    public long id() {
        return id;
    }

    public Proposal proposed() {
        return proposed;
    }

    public Proposal promised() {
        return promised;
    }
}
