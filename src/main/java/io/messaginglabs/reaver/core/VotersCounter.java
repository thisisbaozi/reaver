package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.utils.AddressUtils;
import java.util.ArrayList;
import java.util.List;

public class VotersCounter {

    private long instanceId;
    private List<Long> accepted = new ArrayList<>();
    private List<Long> refused = new ArrayList<>();

    public void addAccepted(long nodeId) {
        addIfAbsent(accepted, nodeId);
    }

    public void addRefused(long nodeId) {
        addIfAbsent(refused, nodeId);
    }

    private void addIfAbsent(List<Long> nodes, long nodeId) {
        if (!nodes.contains(nodeId)) {
            nodes.add(nodeId);
        }
    }

    public int accepted() {
        return accepted.size();
    }

    public int refused() {
        return refused.size();
    }

    public String dumpRefused() {
        return dump(refused);
    }

    public String dumpAccepted() {
        return dump(accepted);
    }

    private String dump(List<Long> nodes) {
        StringBuilder str = new StringBuilder("(");

        int size = nodes.size();
        for (int i = 0; i < size; i++) {
            str.append(AddressUtils.toString(nodes.get(i)));
            if (i < size - 1) {
                str.append(",");
            }
        }

        str.append(")");
        return str.toString();
    }

    public void reset(long instanceId) {
        this.instanceId = instanceId;
        this.accepted.clear();
        this.refused.clear();
    }

    @Override
    public String toString() {
        return "VotersCounter{" +
            "instanceId=" + instanceId +
            ", accepted=" + dumpAccepted() +
            ", refused=" + dumpRefused() +
            '}';
    }

}
