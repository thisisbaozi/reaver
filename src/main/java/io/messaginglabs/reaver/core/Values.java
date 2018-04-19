package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.utils.Parameters;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class Values implements Iterable<Value> {

    /*
     * data format:
     *
     *
     */
    private ByteBuf data;
    private int groupId;
    private long instanceId = Defines.VOID_INSTANCE_ID;
    private long proposer;
    private List<Value> payload;

    public Values(ByteBuf data) {
        this.data = Parameters.requireNotEmpty(data);
        this.parse();
    }

    private void parse() {
        Objects.requireNonNull(data, "data");

        ByteBuf slice = data.slice();

        int groupId = slice.readInt();
        long instanceId = slice.readLong();
        long proposer = slice.readLong();
        int size = slice.readInt();
        if (size <= 0){
            throw new IllegalArgumentException("malformed size: " + size);
        }

        if (payload == null) {
            payload = new ArrayList<>(size);
        }

        for (int i = 0; i < size; i++) {

        }

        this.groupId = groupId;
        this.instanceId = instanceId;
        this.proposer = proposer;
    }

    @Override
    public Iterator<Value> iterator() {
        if (payload == null) {
            throw new IllegalStateException("no payload");
        }

        return payload.iterator();
    }

    public long instanceId() {
        return instanceId;
    }

    public int size() {
        return payload != null ?  payload.size() : 0;
    }

    public Value get(int i) {
         return payload == null ? null : payload.get(i);
    }
}
