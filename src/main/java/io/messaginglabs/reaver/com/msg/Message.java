package io.messaginglabs.reaver.com.msg;

public abstract class Message {


    public enum Type {
        PREPARE(1),

        ;

        public final int value;

        Type(int value) {
            this.value = value;
        }
    }

    private int groupId;

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getGroupId() {
        return groupId;
    }

    abstract public Type type();

    public class Join {

    }


    public class Proposal {

    }

}
