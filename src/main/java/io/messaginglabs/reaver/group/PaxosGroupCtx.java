package io.messaginglabs.reaver.group;

public class PaxosGroupCtx implements GroupContext {

    private long maxChosenInstanceId;
    private long maxSeenInstanceId;

    @Override
    public long maxSerialChosenInstanceId() {
        return maxChosenInstanceId;
    }

    @Override
    public long maxSerialChosenInstanceId(long id) {
        return maxChosenInstanceId = id;
    }

    @Override
    public long maxSeenInstanceId() {
        return maxSeenInstanceId;
    }

    @Override
    public long maxSeenInstanceId(long id) {
        return maxSeenInstanceId;
    }
}
