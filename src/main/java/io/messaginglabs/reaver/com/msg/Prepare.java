package io.messaginglabs.reaver.com.msg;

public class Prepare extends Message {

    @Override
    public Type type() {
        return Type.PREPARE;
    }
}
