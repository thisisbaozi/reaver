package io.messaginglabs.reaver.group;

public interface GroupContext {


    long maxSerialChosenInstanceId();
    long maxSerialChosenInstanceId(long id);
    long maxSeenInstanceId();
    long maxSeenInstanceId(long id);

}
