package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.Prepare;
import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Propose;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import java.util.Objects;

public class ParallelAcceptor extends AlgorithmParticipant implements Acceptor {

    private final InstanceCache cache;

    /*
     * As a optimization for reducing memory footprint, this acceptor
     * is able to reuse both objects, callers shouldn't rely on returned
     * reply
     */
    private final AcceptorReply reply;

    public ParallelAcceptor(InternalPaxosGroup group) {
        super(group);

        this.cache = group.cache();
        if (this.cache == null) {
            throw new IllegalArgumentException("buggy, group has no instances cache");
        }

        this.reply = new AcceptorReply();
    }

    @Override
    public AcceptorReply process(Prepare msg) {
        Objects.requireNonNull(msg, "reply");

        PaxosInstance instance = get(msg.getInstanceId());

        // Tells the proposer proposed this reply msg if the instance is
        // finished
        if (instance.isDone()) {
            return null;
        }

        Proposal proposal = instance.accepted();
        Ballot.CompareResult result = proposal.compare(msg.getSequence(), msg.getNodeId());

        reply.setSequence(proposal.getSequence());
        reply.setNodeId(proposal.getNodeId());
        reply.setInstanceId(msg.getInstanceId());
        reply.setAcceptorId(group().local().id());

        if (result.isSmaller()) {
            reply.setOp(Opcode.REJECT_PREPARE);
        } else {
            // promise do not accept proposals which's id is smaller
            // this one.
            proposal.setSequence(msg.getSequence());
            proposal.setNodeId(msg.getNodeId());

            if (proposal.getValue() != null) {
                // this acceptor has accepted a value, tell the proposer
                // posted this msg that it should process the value first
                reply.setOp(Opcode.PREPARE_REPLY);
                reply.setValue(proposal.getValue());
            } else {
                reply.setOp(Opcode.PREPARE_EMPTY_REPLY);
            }
        }

        return reply;
    }


    @Override
    public AcceptorReply process(Propose msg) {
        Objects.requireNonNull(msg, "propose");

        PaxosInstance instance = get(msg.getInstanceId());

        if (instance.isDone()) {
            return null;
        }

        Proposal proposal = instance.accepted();
        Ballot.CompareResult result = proposal.compare(msg.getSequence(), msg.getNodeId());

        reply.setInstanceId(msg.getInstanceId());
        reply.setSequence(proposal.getSequence());
        reply.setAcceptorId(group().local().id());
        reply.setNodeId(proposal.getNodeId());

        if (result.isSmaller()) {
            reply.setOp(Opcode.REJECT_ACCEPT);
        } else {
            // Do logging

            proposal.setNodeId(msg.getNodeId());
            proposal.setSequence(msg.getSequence());
            proposal.setValue(msg.getValue());

            reply.setOp(Opcode.ACCEPT_REPLY);
        }

        return reply;
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

    @Override public void close() throws Exception {

    }
}
