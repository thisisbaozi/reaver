package io.messaginglabs.reaver;

import io.messaginglabs.reaver.dsl.ChosenValues;
import io.messaginglabs.reaver.dsl.CommitResult;
import io.messaginglabs.reaver.dsl.PaxosError;
import io.messaginglabs.reaver.dsl.Snapshot;
import io.messaginglabs.reaver.dsl.SnapshotBuilder;
import io.messaginglabs.reaver.dsl.StateMachine;
import io.messaginglabs.reaver.dsl.ValueCtx;
import java.util.Iterator;

public class EmptyStateMachine implements StateMachine {
    @Override public void apply(Iterator<ChosenValues> instances) {

    }

    @Override public void process(CommitResult cause, ValueCtx value) {

    }

    @Override public void onPanic(PaxosError error) {

    }

    @Override public void install(Snapshot snapshot) {

    }

    @Override public void create(SnapshotBuilder builder) {

    }

    @Override public void onGroupClosed() {

    }
}
