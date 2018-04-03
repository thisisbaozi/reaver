package io.messaginglabs.reaver.core;

import io.netty.buffer.ByteBuf;
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

    /*
     * the acceptor has promised that do not accept any proposals less
     * than this for this instance
     */
    protected Proposal acceptor;

    // chosen value
    protected ByteBuf chosen;

    /*
     * -1 means no one holds this instance, otherwise it's the id of
     * a proposer
     */
    protected int holder;

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

    public Proposal acceptor() {
        return acceptor;
    }

    public ByteBuf chosenValue(){
        return chosen;
    }

    public void choose(ByteBuf value) {
        this.chosen = value;
    }

    public int hold(int proposerId) {
        if (holder != -1) {
            return holder;
        }

        holder = proposerId;
        return -1;
    }

    public boolean isChosen() {
        return false;
    }
}
