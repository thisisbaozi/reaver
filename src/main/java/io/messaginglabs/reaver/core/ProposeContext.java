package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.config.PaxosConfig;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProposeContext {

    private int groupId;
    private long commit;

    /*
     * begin: when this proposal propose
     * end: when this proposal completed
     */
    private long begin;
    private long end;

    /*
     * times this proposal propose due to conflicts or other
     * reasons(e.g: network trouble).
     */
    private int times;

    private PaxosPhase stage = PaxosPhase.READY;


    private List<GenericCommit> commits = new ArrayList<>();
    private ByteBuf buffer;

    // the id of Paxos instance this proposal associated with
    private long instanceId = Defines.VOID_INSTANCE_ID;
    private int sequence;

    // the instance propose this proposal based on the config
    private PaxosConfig config;
    private AlgorithmPhase phase = AlgorithmPhase.TWO_PHASE;
    private ByteBuf tmpValue;

    private final Ballot propose = new Ballot();
    private final Ballot accept = new Ballot();
    private final Ballot choose = new Ballot();
    private final Ballot greatestSeen = new Ballot();

    private final BallotsCounter counter = new BallotsCounter();

    public ProposeContext(ByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");

        if (buffer.isReadOnly()) {
            throw new IllegalArgumentException("read only buffer");
        }

        this.buffer = buffer;
    }

    public void setCommits(List<GenericCommit> commits) {
        Objects.requireNonNull(commits, "commits");

        if (commits.isEmpty()) {
            throw new IllegalArgumentException("no commits");
        }

        if (stage != PaxosPhase.READY) {
            throw new IllegalStateException(
                String.format("Paxos currentPhase is not ready(%s)", stage.name())
            );
        }

        if (commits != this.commits) {
            if (this.commits.size() > 0) {
                throw new IllegalStateException(
                    String.format("commits cache is not empty(%d)", this.commits.size())
                );
            }

            this.commits.addAll(commits);
        }

        this.commit = System.currentTimeMillis();
    }

    private void mergeValue() {
        // reserve a number of bytes used to save Paxos information:
        // 0. group id
        // 1. instance id
        // 2. the number of entries
        this.buffer.writeInt(groupId);
        this.buffer.writeLong(instanceId);
        this.buffer.writeInt(this.commits.size());
        int size = this.commits.size();
        for (int i = 0; i < size; i++) {
            GenericCommit commit = this.commits.get(i);

            ByteBuf value = commit.value();
            if (value.refCnt() == 0) {
                throw new IllegalStateException(
                    String.format("buggy, the count of ref of buffer(%s) at %d is 0", commit.toString(), i)
                );
            }

            this.buffer.writeBytes(value);

            /*
             * proposer doesn't rely on this buffer any more, release it.
             */
            value.release();
        }
    }

    public void reset(long instanceId, PaxosConfig config, long nodeId) {
        if (commits.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("nothing needs to reach a consensus for instance(%d)", instanceId)
            );
        }

        // Checks whether or not there's already one proposal is in progress
        if (this.instanceId != Defines.VOID_INSTANCE_ID) {
            throw new IllegalStateException(
                String.format("instance(%d) is in progress, can't propose a new one(%d)", this.instanceId, instanceId)
            );
        }

        this.instanceId = instanceId;
        this.config = config;
        this.begin = System.currentTimeMillis();
        this.end = 0;
        this.times = 0;
        this.sequence = 0;
        this.propose.setNodeId(nodeId);
        this.propose.setSequence(0);
        this.mergeValue();
    }

    public void clear() {

    }

    public void setOtherValue(ByteBuf value) {
        this.tmpValue = value;
    }

    public List<GenericCommit> valueCache() {
        return commits;
    }

    public ByteBuf value() {
        return buffer;
    }

    public int delay() {
        times++;
        return times;
    }

    public int timesDelayed() {
        return times;
    }

    public AlgorithmPhase phase() {
        return phase;
    }

    public void setPhase(AlgorithmPhase phase) {
        this.phase = phase;
    }

    public PaxosConfig config() {
        return config;
    }

    public long instanceId() {
        return instanceId;
    }

    public PaxosInstance instance() {
        return null;
    }

    public void instanceId(long instanceId) {
        this.instanceId = instanceId;
    }

    public BallotsCounter counter() {
        return counter;
    }

    public void begin(PaxosPhase stage) {
        this.begin = System.currentTimeMillis();
        this.stage = stage;
    }

    public long begin() {
        return begin;
    }

    public long delayed() {
        if (begin == 0) {
            /*
             * nothing propose
             */
            return -1;
        }

        return System.currentTimeMillis() - begin;
    }

    public PaxosPhase currentPhase() {
        return stage;
    }


    public long nodeId() {
        return 0;
    }

    public String dumpChosenInstance() {
        return null;
    }

    public boolean isRefused() {
        return false;
    }


    public PaxosPhase setStage(PaxosPhase stage) {
        Objects.requireNonNull(stage, "currentPhase");

        PaxosPhase current = this.stage;
        this.stage = current;

        return current;
    }

    public void setGreatestSeen(long nodeId, int sequence) {
        if (greatestSeen.compare(sequence, nodeId).isSmaller()) {
            greatestSeen.setNodeId(nodeId);
            greatestSeen.setSequence(sequence);
        }
    }

    public Ballot getGreatestSeen() {
        return greatestSeen;
    }

    public Ballot accept() {
        return accept;
    }

    public Ballot choose() {
        return choose;
    }

    public void setChooseBallot(Ballot ballot) {
        choose.setSequence(ballot.getSequence());
        choose.setNodeId(ballot.getNodeId());
    }

    public Ballot ballot() {
        return propose;
    }

    public int maxSequence() {
        return 0;
    }

}
