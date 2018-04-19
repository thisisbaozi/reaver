package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.dsl.ChosenValue;
import io.messaginglabs.reaver.dsl.ChosenValues;
import io.messaginglabs.reaver.utils.Parameters;
import io.netty.buffer.ByteBuf;
import java.util.List;

public class DefaultChosenValues implements ChosenValues {

    private final long instanceId;
    private final Values values;
    private final List<GenericCommit> commits;

    public DefaultChosenValues(long instanceId, ByteBuf data, List<GenericCommit> commits) {
        if (instanceId == Defines.VOID_INSTANCE_ID) {
            throw new IllegalArgumentException("void instance id");
        }

        Values values = new Values(data);
        if (values.size() != commits.size()) {
            throw new IllegalArgumentException(
                String.format(
                    "buggy, the number of values(%d) in instance(%d) is not equals to the number of commits(%d)",
                    values.size(),
                    instanceId,
                    commits.size()
                )
            );
        }

        this.instanceId = instanceId;
        this.commits = commits;
        this.values = values;
    }

    @Override
    public long instanceId() {
        return instanceId;
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public ChosenValue get(int idx) {
        if (idx < 0) {
            throw new IllegalArgumentException("void idx: " + idx);
        }

        if (idx >= size()) {
            throw new IllegalArgumentException(String.format(
                "there's %d values in the instance(%d), %d is not exists",
                size(),
                instanceId,
                idx
            ));
        }

        Value value = values.get(idx);
        if (value == null) {
            throw new IllegalStateException(
                String.format("can't get the value in the %d(%d)", idx, values.size())
            );
        }

        Object attachment = null;
        if (commits != null) {
            GenericCommit commit = commits.get(idx);
            attachment = commit.attachment();
        }

        return new DefaultChosenValue(value.appData(), attachment);
    }

    class DefaultChosenValue implements ChosenValue {

        private final ByteBuf value;
        private final Object attachment;

        DefaultChosenValue(ByteBuf value, Object attachment) {
            this.value = Parameters.requireNotEmpty(value);
            this.attachment = attachment;
        }

        @Override
        public ByteBuf data() {
            return value;
        }

        @Override
        public Object attachment() {
            return attachment;
        }
    }

}
