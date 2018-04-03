package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Propose;
import io.messaginglabs.reaver.dsl.PaxosGroup;
import io.messaginglabs.reaver.group.InternalPaxosGroup;
import io.messaginglabs.reaver.utils.AddressUtils;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelAcceptor extends AlgorithmParticipant implements Acceptor {

    private static final Logger logger = LoggerFactory.getLogger(ParallelProposer.class);

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

    private AcceptorReply processPrepare(Propose msg, PaxosInstance instance) {
        Proposal proposal = instance.acceptor();
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
                // this acceptor has acceptor a value, tell the proposer
                // posted this msg that it should process the value first
                reply.setOp(Opcode.PREPARE_REPLY);
                reply.setValue(proposal.getValue());
            } else {
                reply.setOp(Opcode.PREPARE_EMPTY_REPLY);
            }
        }

        return reply;
    }

    private AcceptorReply processPropose(Propose msg, PaxosInstance instance) {
        Proposal proposal = instance.acceptor();
        Ballot.CompareResult result = proposal.compare(msg.getSequence(), msg.getNodeId());

        reply.setInstanceId(msg.getInstanceId());
        reply.setSequence(proposal.getSequence());
        reply.setAcceptorId(group().local().id());
        reply.setNodeId(proposal.getNodeId());

        if (result.isSmaller()) {
            reply.setOp(Opcode.REJECT_ACCEPT);
        } else {
            proposal.setNodeId(msg.getNodeId());
            proposal.setSequence(msg.getSequence());
            proposal.setValue(msg.getValue());

            reply.setOp(Opcode.ACCEPT_REPLY);
        }

        return reply;
    }

    @Override
    public AcceptorReply process(Propose msg) {
        Objects.requireNonNull(msg, "propose");

        if (group.role() != PaxosGroup.Role.FORMAL) {
            if (isDebug()) {
                logger.debug("group can't vote for any proposal({}), it's ()", msg.toString(), group.role().name());
            }

            return null;
        }

        long instanceId = msg.getInstanceId();
        long maxChosenInstance = group.ctx().maxSerialChosenInstance();
        if (instanceId <= maxChosenInstance) {
            if (isDebug()) {
                logger.debug("the instance({}) from proposer({}) is chosen(max chosen({}))", instanceId, AddressUtils.toString(msg.getNodeId()), maxChosenInstance);
            }

            return null;
        }

        PaxosInstance instance = get(instanceId);
        if (instance.isChosen()) {
            if (isDebug()) {
                logger.debug("instance({}) in group({}) is chosen, ignore ");
            }

            return null;
        }

        Opcode op = msg.op();
        if (op.isPrepare()) {
            return processPrepare(msg, instance);
        } else if (op.isPropose()) {
            return processPropose(msg, instance);
        } else {
            throw new IllegalArgumentException("unknown op: " + op.name());
        }
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

    @Override
    public void close() throws Exception {

    }
}
