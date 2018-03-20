package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.Accept;
import io.messaginglabs.reaver.com.AcceptReply;
import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.com.msg.Prepare;
import io.messaginglabs.reaver.com.PrepareReply;
import io.messaginglabs.reaver.debug.RunningEvent;
import io.messaginglabs.reaver.debug.RunningEvents;
import io.messaginglabs.reaver.group.MultiPaxosGroup;
import java.util.Objects;

public class ParallelAcceptor extends AlgorithmVoter implements Acceptor {

    private class InstancePromise {

    }

    private class Promises {

    }

    @Override public PrepareReply process(Prepare prepare) {
        return null;
    }

    @Override
    public AcceptReply process(Accept accept) {
        return null;
    }


    @Override
    public MultiPaxosGroup group() {
        return null;
    }

    @Override public boolean isDebug() {
        return false;
    }

    @Override public RunningEvents events() {
        return null;
    }

    @Override public void add(RunningEvent event) {

    }
}
