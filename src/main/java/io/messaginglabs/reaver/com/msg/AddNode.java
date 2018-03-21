package io.messaginglabs.reaver.com.msg;

public class AddNode extends Message {
    @Override
    public Operation op() {
        return Operation.ADD_NODE;
    }
}
