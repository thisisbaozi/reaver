package io.messaginglabs.reaver.com.msg;

public class Prepare extends Message {

    @Override
    public Operation op() {
        return Operation.PREPARE;
    }
}
