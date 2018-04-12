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

        if (result.isGreater()) {
            reply.setOp(Opcode.REJECT_PREPARE);
        } else {
            // promise do not accept proposals which's id is smaller
            // this one.
            proposal.setSequence(msg.getSequence());
            proposal.setNodeId(msg.getNodeId());

            if (proposal.getValue() != null) {
                // this acceptor has acceptor a myValue, tell the proposer
                // posted this msg that it should process the myValue first
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
        reply.setGroupId(msg.getGroupId());

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

        /*
         * two things we need to check before voting for any proposal:
         *
         * 0. this node is a FORMAL participant instead of a UNKNOWN or FOLLOWER node.
         * 1. the instance id the given message contains is not a chosen one.
         */
        if (group.role() != PaxosGroup.Role.FORMAL) {
            if (isDebug()) {
                logger.debug("group can't vote for any proposal({}), it's ()", msg.toString(), group.role().name());
            }

            return null;
        }

        long instanceId = msg.getInstanceId();
        if (instanceId == Defines.VOID_INSTANCE_ID) {
            throw new IllegalArgumentException("buggy, void instance id in msg: " + msg.toString());
        }

        if (isDebug()) {
            logger.debug(
                "the acceptor(group={}, promised={}) starts to process the message({}/{}/{}:{}/{}) from proposer({})",
                group.id(),
                msg.getType().name(),
                msg.getInstanceId(),
                msg.getNodeId(),
                msg.getSequence(),
                msg.getValue().readableBytes(),
                AddressUtils.toString(msg.getNodeId())
            );
        }

        long maxChosenInstance = group.ctx().maxSerialChosenInstanceId();
        if (instanceId <= maxChosenInstance) {
            if (isDebug()) {
                logger.debug(
                    "the instance({}) from proposer({}) is chosen(max chosen({}))",
                    instanceId,
                    AddressUtils.toString(msg.getNodeId()),
                    maxChosenInstance
                );
            }

            return null;
        }

        PaxosInstance instance = get(instanceId);
        if (instance.isChosen()) {
            if (isDebug()) {
                logger.debug(
                    "the instance({}) has chosen a myValue, ignore message({}) from proposer({})",
                    instanceId,
                    msg.getType().name(),
                    AddressUtils.toString(msg.getNodeId())
                );
            }

            // the proposer sent this message should learn the chosen instance ASAP
            reply.setInstanceId(instanceId);
            reply.setGroupId(msg.getGroupId());
            reply.setOp(Opcode.CHOOSE_VALUE);
            reply.setValue(instance.chosenValue());
            return reply;
        }

        Opcode op = msg.op();
        if (op.isPrepare()) {
            return processPrepare(msg, instance);
        } else if (op.isPropose()) {
            return processPropose(msg, instance);
        }

        throw new IllegalArgumentException("unknown op: " + op.name());
    }

    private PaxosInstance get(long instanceId) {
        PaxosInstance instance = cache.get(instanceId);
        if (instance == null) {
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
