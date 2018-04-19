package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.dsl.CheckpointStateMachine;
import io.messaginglabs.reaver.dsl.ChosenValues;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ChosenValuesApplier implements Applier {

    private final int groupId;
    private final StateMachine dst;

    private BlockingQueue<PaxosInstance> instances;
    private long checkpoint = 0;
    private long flushCheckpoint = 0;
    private List<ChosenValues> cache;
    private List<PaxosInstance> batch;

    public ChosenValuesApplier(StateMachine dst, int groupId) {
        this.dst = Objects.requireNonNull(dst, "dst");

        if (this.dst instanceof CheckpointStateMachine) {
            CheckpointStateMachine sm = (CheckpointStateMachine)dst;
            checkpoint = flushCheckpoint = sm.getCheckpoint();
        }

        this.groupId = groupId;

        this.cache = new ArrayList<>();
        this.batch = new ArrayList<>();
        this.instances = new LinkedBlockingQueue<>();
    }

    public int apply() {
        int count = 0;
        for (;;) {
            PaxosInstance instance;
            try {
                instance = instances.poll(1000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                break;
            }

            if (instance == null) {
                break;
            }

            batch.add(instance);
        }

        if (batch.size() > 0) {
            try {
                apply(batch);
            } finally {
                batch.clear();
            }
        }

        return count;
    }

    private void apply(List<PaxosInstance> batch) {
        for (PaxosInstance instance : batch) {
            cache.add(new DefaultChosenValues(instance.id(), instance.chosen().getValue(), instance.commits()));
        }

        if (cache.size() > 0) {
            try {
                execute(cache);
            } finally {
                cache.clear();
            }
        }
    }

    private void execute(List<ChosenValues> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("no values");
        }

        long instanceId = values.get(values.size() - 1).instanceId();
        dst.apply(values.iterator());

        if (dst instanceof CheckpointStateMachine) {
            CheckpointStateMachine stateMachine = (CheckpointStateMachine)dst;
            if (stateMachine.flushRightNow()) {
                stateMachine.flush(instanceId);
            }
        }
    }

    @Override
    public void add(PaxosInstance instance) {
        Objects.requireNonNull(instance, "instance");

        if (instance.refCnt() == 0) {
            throw new IllegalArgumentException(String.format("instance(%d) is released", instance.id()));
        }

        if (!instance.isChosen()) {
            throw new IllegalArgumentException(
                String.format("instance(%d-%d) has no chosen value", instance.id(), groupId)
            );
        }

        ByteBuf value = instance.chosen().getValue();
        if (value.refCnt() == 0 || value.readableBytes() == 0) {
            throw new IllegalArgumentException(
                String.format(
                    "invalid value(%d/%d) in instance(%d) of group(%d)",
                    value.refCnt(),
                    value.readableBytes(),
                    instance.id(),
                    groupId
                )
            );
        }

        instance.retain();
        if (!instances.offer(instance)) {
            instance.release();
            throw new IllegalStateException(
                String.format("can't push new value to queue(%d)", instances.size())
            );
        }
    }


}
