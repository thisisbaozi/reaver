package io.messaginglabs.reaver.core;

public class Ballot {

    private int sequence;
    private long node;

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public long getNode() {
        return node;
    }

    public void setNode(long node) {
        this.node = node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Ballot ballot = (Ballot)o;

        return sequence == ballot.sequence && node == ballot.node;
    }

    @Override
    public int hashCode() {
        int result = sequence;
        result = 31 * result + (int)(node ^ (node >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Ballot{" +
            "sequence=" + sequence +
            ", node=" + node +
            '}';
    }
}
