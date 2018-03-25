package io.messaginglabs.reaver.core;

import io.netty.buffer.ByteBuf;

public class Value {

    private int groupId;

    /*
     * the instance id all nodes in the config have reached a consensus on
     */
    private long instanceId;

    /*
     * indicates whether this value is chosen or not.
     */
    private boolean chosen;
    private ByteBuf data;

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

    public boolean isChosen() {
        return chosen;
    }

    public void setChosen(boolean chosen) {
        this.chosen = chosen;
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Value{" +
            "groupId=" + groupId +
            ", instanceId=" + instanceId +
            ", chosen=" + chosen +
            ", data=" + data +
            '}';
    }

}
