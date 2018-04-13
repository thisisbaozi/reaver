package io.messaginglabs.reaver.core;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import java.util.List;

public class PaxosInstance extends AbstractReferenceCounted {

    /*
     * the group this Paxos instance belongs to
     */
    protected int groupId;

    /*
     * unique id in group
     */
    protected long id;

    /*
     * the acceptor has promised that do not accept any proposals less
     * than this for this instance
     */
    protected Proposal acceptor = new Proposal();

    protected Proposal chosen;
    protected List<GenericCommit> commits;

    /*
     * -1 means no one holds this instance, otherwise it's the id of
     * a proposer
     */
    protected int holder;

    public void reset(long id) {
        this.id = id;
    }

    /**
     * Returns true if the value of the Paxos instance is chosen.
     *
     * DONE means the members in a specified config have reached a consensus
     * on this instance, the value has been chosen.
     */
    public boolean hasChosen() {
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

    public Proposal acceptor() {
        return acceptor;
    }

    public Proposal chosen() {
        return chosen;
    }

    public void choose(Proposal proposal) {
        this.chosen = proposal;
        if (this.acceptor != proposal) {
            this.acceptor = proposal;
        }
    }

    public int hold(int proposerId) {
        if (holder != -1) {
            return holder;
        }

        holder = proposerId;
        return -1;
    }

    public boolean isChosen() {
        return chosen != null;
    }

    public List<GenericCommit> commits() {
        return commits;
    }
}
