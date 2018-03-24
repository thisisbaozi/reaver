package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.com.msg.Prepare;
import io.messaginglabs.reaver.com.msg.PrepareReply;
import io.messaginglabs.reaver.com.msg.Propose;
import io.messaginglabs.reaver.com.msg.ProposeReply;
import io.messaginglabs.reaver.group.PaxosGroup;
import java.util.Objects;

public class ParallelAcceptor extends AlgorithmParticipant implements Acceptor {

    private final InstanceCache cache;

    /*
     * As a optimization for reducing memory footprint, this acceptor
     * is able to reuse both objects, callers shouldn't rely on returned
     * reply
     */
    private final PrepareReply prepare;
    private final ProposeReply accept;

    public ParallelAcceptor(PaxosGroup group) {
        super(group);

        this.cache = group.cache();
        if (this.cache == null) {
            throw new IllegalArgumentException("buggy, group has no instances cache");
        }

        this.prepare = new PrepareReply();
        this.accept = new ProposeReply();
    }

    @Override
    public PrepareReply process(Prepare msg) {
        Objects.requireNonNull(msg, "prepare");

        PaxosInstance instance = get(msg.getInstanceId());

        // Tells the proposer proposed this prepare msg if the instance is
        // finished
        if (instance.isDone()) {
            return null;
        }

        Proposal proposal = instance.accepted();
        Ballot.CompareResult result = proposal.compare(msg.getSequence(), msg.getNodeId());

        prepare.setSequence(proposal.getSequence());
        prepare.setNodeId(proposal.getNodeId());
        prepare.setInstanceId(msg.getInstanceId());
        prepare.setAcceptor(group().local().id());

        if (result.isSmaller()) {
            prepare.setOp(Message.Operation.REJECT_PREPARE);
        } else {
            // promise do not accept proposals which's id is smaller
            // this one.
            proposal.setSequence(msg.getSequence());
            proposal.setNodeId(msg.getNodeId());

            if (proposal.getValue() != null) {
                // this acceptor has accepted a value, tell the proposer
                // posted this msg that it should process the value first
                prepare.setOp(Message.Operation.PREPARE_REPLY);
                prepare.setValue(proposal.getValue());
            } else {
                prepare.setOp(Message.Operation.PREPARE_EMPTY_REPLY);
            }
        }

        return prepare;
    }


    @Override
    public ProposeReply process(Propose msg) {
        Objects.requireNonNull(msg, "propose");

        PaxosInstance instance = get(msg.getInstanceId());

        if (instance.isDone()) {
            return null;
        }

        Proposal proposal = instance.accepted();
        Ballot.CompareResult result = proposal.compare(msg.getSequence(), msg.getNodeId());

        accept.setInstanceId(msg.getInstanceId());
        accept.setSequence(proposal.getSequence());
        accept.setAcceptorId(group().local().id());
        accept.setNodeId(proposal.getNodeId());

        if (result.isSmaller()) {
            accept.setOp(Message.Operation.REJECT_ACCEPT);
        } else {
            // Do logging

            proposal.setNodeId(msg.getNodeId());
            proposal.setSequence(msg.getSequence());
            proposal.setValue(msg.getValue());

            accept.setOp(Message.Operation.ACCEPT_REPLY);
        }

        return accept;
    }

    private PaxosInstance get(long instanceId) {
        PaxosInstance instance = cache.get(instanceId);
        if (instance == null) {
            /*
             * it's a new instance
             */
            instance = cache.createIfAbsent(instanceId);
            if (instance == null) {
                throw new IllegalStateException(
                    String.format("can't create a instance(%d)", instanceId)
                );
            }
        }

        return instance;
    }



}
