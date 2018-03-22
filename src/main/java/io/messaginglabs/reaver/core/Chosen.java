package io.messaginglabs.reaver.core;

public abstract class Chosen {

    private int groupId;

    /*
     * the instance id all nodes in the config have reached a consensus on
     */
    private long instanceId;

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(long instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Chosen value = (Chosen)o;

        return groupId == value.groupId && instanceId == value.instanceId;
    }

    @Override
    public int hashCode() {
        int result = groupId;
        result = 31 * result + (int)(instanceId ^ (instanceId >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Chosen{" +
            "groupId=" + groupId +
            ", instanceId=" + instanceId +
            '}';
    }
}
