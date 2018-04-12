package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.config.PaxosConfig;
import io.messaginglabs.reaver.utils.AddressUtils;

public class BallotsCounter {

    private static final int NODES_CAPACITY = (1 << 8) - 1;

    /*
     * Using array instead of HashSet/ArrayList is for reducing memory footprint
     */
    private int nodes = 0;
    private long[] nodesAnswered = new long[Defines.MAX_ACCEPTORS];

    /*
     * 6 bits to maintain how many nodes rejected or promised
     */
    private int nodesRejected = 0;
    private int nodesPromised = 0;

    private int parseCount(int ctx) {
        return NODES_CAPACITY & ctx;
    }

    private boolean parseState(int ctx, int idx) {
        int bit = 0x1 << ((Integer.SIZE - idx) - 1);
        return (ctx & bit) == bit;
    }

    private int count(int ctx, int idx) {
        // increase nodes
        ctx = ctx + 1;

        // mark
        int bit = 0x1 << ((Integer.SIZE - idx) - 1);

        if ((bit & ctx) == bit) {
            throw new IllegalStateException(
                String.format("bit at %d of myValue(%s) is already marked", idx, Integer.toBinaryString(ctx))
            );
        }

        ctx |= bit;
        return ctx;
    }

    private int add(long node) {
        if (nodes >= Defines.MAX_ACCEPTORS) {
            return -1;
        }

        for (int i = 0; i < nodes; i++) {
            if (nodesAnswered[i] == node) {
                return -1;
            }
        }

        nodesAnswered[nodes] = node;
        nodes++;

        return nodes - 1;
    }

    public void countRejected(long node) {
        int idx;
        if ((idx = add(node)) != -1) {
            nodesRejected = count(nodesRejected, idx);
        }
    }

    public void countPromised(long node) {
        int idx;
        if ((idx = add(node)) != -1) {
            nodesPromised = count(nodesPromised, idx);
        }
    }

    public int nodesRejected() {
        return parseCount(nodesRejected);
    }

    public int nodesPromised() {
        return parseCount(nodesPromised);
    }

    public int nodesAnswered() {
        return nodes;
    }

    public String dumpRejected() {
        return dump(nodesRejected);
    }

    public String dumpAccepted() {
        return dump(nodesPromised);
    }

    private String dump(int ctx) {
        StringBuilder str = new StringBuilder("(");

        int count = parseCount(ctx);
        for (int i = 0; i < nodes; i++) {
            if (!parseState(ctx, i)) {
                continue;
            }

            str.append(AddressUtils.toString(nodesAnswered[i]));
            if (count > 1) {
                str.append(", ");
            }

            count--;
            if (count == 0) {
                break;
            }
        }

        str.append(")");
        return str.toString();
    }

    public long at(int idx) {
        if (idx >= nodes) {
            throw new ArrayIndexOutOfBoundsException(
                String.format("nodes(%d), idx(%d)", nodes, idx)
            );
        }

        return nodesAnswered[idx];
    }

    public void reset() {
        nodes = 0;
        nodesPromised = 0;
        nodesRejected = 0;
    }

    public boolean acceptInMajority(PaxosConfig cfg) {
        return true;
    }

    @Override
    public String toString() {
        return "BallotsCounter{" +
            ", nodesAccepted=" + dumpAccepted() +
            ", nodesRejected=" + dumpRejected() +
            '}';
    }

}
