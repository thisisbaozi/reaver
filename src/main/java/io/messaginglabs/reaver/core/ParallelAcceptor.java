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

public class ParallelAcceptor implements Acceptor {

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



    public AcceptReply processAccept(Message.Proposal proposal) {
        Objects.requireNonNull(proposal, "proposal");

        /*
         * debug information
         */

        /*
         * statistics
         */

        /*
         * fail point
         */

        AcceptReply result = doAccept(proposal);

        /*
         * fail point
         */

        return result;
    }

    private AcceptReply doAccept(Message.Proposal proposal) {


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
