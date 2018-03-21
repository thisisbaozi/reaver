package io.messaginglabs.reaver.core;

public class Ballot {

    protected int sequence;
    protected long nodeId;

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public CompareResult commpare(int sequence, long nodeId) {
        if (sequence == this.sequence && nodeId == this.nodeId) {
            return CompareResult.EQUAL;
        }

        // greater?
        if (this.sequence > sequence || (this.sequence == sequence && this.nodeId > nodeId)) {
            return CompareResult.GREATER;
        }

        return CompareResult.SMALLER;
    }

    public enum CompareResult {
        EQUAL, GREATER, SMALLER;

        public boolean isEquals() {
            return this == EQUAL;
        }

        public boolean isGreater() {
            return this == GREATER;
        }

        public boolean isSmaller() {
            return this == SMALLER;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Ballot ballot = (Ballot)o;

        return sequence == ballot.sequence && nodeId == ballot.nodeId;
    }

    @Override
    public int hashCode() {
        int result = sequence;
        result = 31 * result + (int)(nodeId ^ (nodeId >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Ballot{" +
            "sequence=" + sequence +
            ", nodeId=" + nodeId +
            '}';
    }
}
