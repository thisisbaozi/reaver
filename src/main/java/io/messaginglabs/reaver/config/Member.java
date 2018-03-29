package io.messaginglabs.reaver.config;

public class Member extends Node {

    // which Paxos group this member belongs to
    private int minVersion;
    private int maxVersion;

    public int getMinVersion() {
        return minVersion;
    }

    public void setMinVersion(int minVersion) {
        this.minVersion = minVersion;
    }

    public int getMaxVersion() {
        return maxVersion;
    }

    public void setMaxVersion(int maxVersion) {
        this.maxVersion = maxVersion;
    }

    @Override
    public String toString() {
        return "Member{" +
            ", minVersion=" + minVersion +
            ", maxVersion=" + maxVersion +
            "} " + super.toString();
    }
}
