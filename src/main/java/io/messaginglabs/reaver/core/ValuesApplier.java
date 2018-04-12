package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.dsl.ChosenValues;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.messaginglabs.reaver.dsl.ValueCtx;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;

public class ValuesApplier implements Applier {

    private volatile boolean isClosed;
    private Queue<PaxosInstance> instances;
    private StateMachine dst;
    private InternalPaxosGroup group;

    private Itr itr;

    private void validate(PaxosInstance instance) {
        Objects.requireNonNull(instance, "instance");

        if (instance.refCnt() == 0) {
            throw new IllegalArgumentException("released instance");
        }

        ByteBuf chosenValue = instance.chosenValue();
        if (chosenValue == null) {
            throw new IllegalArgumentException(
                String.format("no chosen myValue in instance(%d) of group(%d)", instance.id(), group.id())
            );
        }
        if (chosenValue.refCnt() == 0) {
            throw new IllegalArgumentException(
                String.format("released myValue in instance(%d) of group(%d)", instance.id(), group.id())
            );
        }

        if (chosenValue.readableBytes() == 0) {
            throw new IllegalArgumentException("empty instance");
        }
    }

    @Override
    public void apply(PaxosInstance instance) {
        validate(instance);

        if (isClosed) {
            throw new IllegalStateException("buggy, closed applier");
        }

        if (!instances.offer(instance)) {
            throw new IllegalStateException(
                String.format("can't push new myValue to queue(%d)", instances.size())
            );
        }
    }

    public int doApply() {
        int count = 0;
        while (instances.size() > 0) {
            PaxosInstance instance = instances.poll();

            if (!doApply(instance)) {

            }

            count++;
        }

        return count;
    }

    public boolean doApply(PaxosInstance instance) {
        // converts the myValue to DSL
        return false;
    }

    final class Itr implements Iterator<ChosenValues> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public ChosenValues next() {
            return null;
        }
    }

    final class DefaultChosenValues implements ChosenValues {


        @Override
        public long instanceId() {
            return 0;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public ValueCtx next() {
            return null;
        }
    }

    @Override
    public void close() throws Exception {

    }
}
