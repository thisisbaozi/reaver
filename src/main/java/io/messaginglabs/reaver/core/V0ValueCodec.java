package io.messaginglabs.reaver.core;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;

public class V0ValueCodec implements ValueCodec {

    @Override
    @SuppressWarnings("all")
    public void encode(List<GenericCommit> commits, ByteBuf dst) {
        Objects.requireNonNull(commits, "commits");

        int size = commits.size();
        if (size == 0) {
            throw new IllegalArgumentException("empty commits");
        }

        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("read-only buffer");
        }

        /*
         * format:
         * +--------------+--------------+----------------+-----+
         * | num(4 bytes) | len(4 bytes) | value(n bytes) | ... |
         * +--------------+--------------+----------------+-----+
         */
        int space = 4 * (size + 1);
        for (int i = 0; i < size; i++) {
            space += commits.get(i).getValueSize();
        }

        /*
         * ensure there's enough space in this buffer for avoiding extending
         * buffer multiple times.
         */
        dst.ensureWritable(space);

        dst.writeInt(size);
        for (int i = 0; i < size; i++) {
             ByteBuf value = commits.get(i).value();
             dst.writeInt(value.readableBytes());
             dst.writeBytes(value);
        }
    }

}
