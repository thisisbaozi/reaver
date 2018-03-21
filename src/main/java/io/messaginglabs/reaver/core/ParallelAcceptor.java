package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.com.msg.Prepare;
import io.messaginglabs.reaver.com.msg.PrepareReply;
import io.messaginglabs.reaver.com.msg.Propose;
import io.messaginglabs.reaver.com.msg.ProposeReply;
import java.util.Objects;

public class ParallelAcceptor extends AlgorithmVoter implements Acceptor {

    private InstanceCache cache;
    private PrepareReply prepareReply;
    private ProposeReply acceptReply;

    @Override
    public PrepareReply process(Prepare prepare) {
        Objects.requireNonNull(prepare, "prepare");

        PaxosInstance instance = get(prepare.getInstanceId());
        // events

        return process(instance, prepare);
    }

    private PrepareReply process(PaxosInstance instance, Prepare prepare) {


        /*
         * the Paxos instance has might be finished.
         */
        if (instance.isDone()) {
            return null;
        }

        int sequence = prepare.getSequence();
        long nodeId = prepare.getNodeId();

        /*
         * execute the first stage(Prepare stage) of Paxos
         */
        Proposal proposal = instance.promised();
        boolean isGreater = proposal.commpare(sequence, nodeId).isGreater();
        if (isGreater) {
            /*
             * this acceptor has promised a proposal from another proposer.
             */
            return null;
        }

        proposal.setNodeId(nodeId);
        proposal.setSequence(sequence);

        prepareReply.setInstanceId(prepareReply.getInstanceId());
        if (proposal.getValue() != null) {
            /*
             * copy accepted value and send it to the proposer proposed the given PREPARE
             */
            prepareReply.setOp(Message.Operation.PREPARE_REPLY);
            prepareReply.setValue(proposal.getValue());
        } else {
            prepareReply.setOp(Message.Operation.PREPARE_EMPTY_REPLY);
        }

        return prepareReply;
    }

    private PaxosInstance get(long instanceId) {
        PaxosInstance instance = cache.get(instanceId);
        if (instance == null) {
            /*
             * it's a new instance
             */
            instance = cache.newInstance(instanceId);
            assert (instance != null);
        }

        return instance;
    }

    @Override
    public ProposeReply process(Propose propose) {
        Objects.requireNonNull(propose, "propose");

        PaxosInstance instance = get(propose.getInstanceId());
        return null;
    }

    private ProposeReply propose(PaxosInstance instance, Propose propose) {
        if (isDone(instance)) {
            return null;
        }

        int sequence = propose.getSequence();
        long nodeId = propose.getNodeId();

        Proposal proposal = instance.promised();
        boolean isGreater = proposal.commpare(sequence, nodeId).isGreater();
        if (isGreater) {
            // ignore
            return null;
        }

        acceptReply.setInstanceId(propose.getInstanceId());
        return acceptReply;
    }

    private boolean isDone(PaxosInstance instance) {
        if (instance.isDone()) {

            return true;
        }
        return false;
    }


}
