package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Proposing;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrentAcceptor extends AlgorithmParticipant implements Acceptor {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentProposer.class);

    private final int groupId;
    private final InstanceCache cache;

    /*
     * As a optimization for reducing memory footprint, this acceptor
     * is able to reuse both objects, callers shouldn't rely on returned
     * reply
     */
    private final AcceptorReply reply;

    public ConcurrentAcceptor(int groupId, long nodeId, InstanceCache cache) {
        if (cache == null) {
            throw new IllegalArgumentException("buggy, group has no instances cache");
        }

        this.groupId = groupId;
        this.cache = cache;
        this.reply = new AcceptorReply();
        this.reply.setGroupId(groupId);
        this.reply.setAcceptorId(nodeId);
    }

    private AcceptorReply processPrepare(Proposing msg, PaxosInstance instance) {
        Proposal proposal = instance.acceptor();
        Ballot.CompareResult result = proposal.compare(msg.getSequence(), msg.getNodeId());

        reply.setSequence(proposal.getSequence());
        reply.setNodeId(proposal.getNodeId());
        reply.setInstanceId(msg.getInstanceId());

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

    private AcceptorReply processAccept(Proposing msg, PaxosInstance instance) {
        Proposal proposal = instance.acceptor();
        Ballot.CompareResult result = proposal.compare(msg.getSequence(), msg.getNodeId());

        reply.setInstanceId(msg.getInstanceId());
        reply.setSequence(proposal.getSequence());
        reply.setNodeId(proposal.getNodeId());

        if (result.isSmaller()) {
            reply.setOp(Opcode.REJECT_ACCEPT);
        } else {
            proposal.setNodeId(msg.getNodeId());
            proposal.setSequence(msg.getSequence());

            ByteBuf current = proposal.getValue();
            if (current != null) {
                current.release();
            } else {
                proposal.setValue(msg.getValue());
            }

            reply.setOp(Opcode.ACCEPT_REPLY);
        }

        return reply;
    }

    @Override
    public AcceptorReply process(Proposing msg) {
        Objects.requireNonNull(msg, "propose");

        /*
         * two things we need to check before voting for any proposal:
         *
         * 0. this node is a FORMAL participant instead of a UNKNOWN or FOLLOWER node.
         * 1. the instance id the given message contains is not a chosen one.
         */
        long instanceId = msg.getInstanceId();
        if (instanceId == Defines.VOID_INSTANCE_ID) {
            throw new IllegalArgumentException("buggy, void instance id in msg: " + msg.toString());
        }

        if (isDebug()) {
            logger.debug(
                "the acceptor(group={}, promised={}) starts to process the message({}/{}/{}:{}/{}) from proposer({})",
                groupId,
                msg.getType().name(),
                msg.getInstanceId(),
                msg.getNodeId(),
                msg.getSequence(),
                msg.getValue().readableBytes(),
                AddressUtils.toString(msg.getNodeId())
            );
        }

        /*
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
        */

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
            reply.setOp(Opcode.LEARN_CHOSEN_VALUE);
            //reply.setValue(instance.chosen());
            return reply;
        }

        Opcode op = msg.getOp();
        if (op.isPrepare()) {
            return processPrepare(msg, instance);
        } else if (op.isPropose()) {
            return processAccept(msg, instance);
        }

        throw new IllegalArgumentException("unknown getOp: " + op.name());
    }

    public PaxosInstance get(long instanceId) {
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

}
