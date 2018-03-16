package io.messaginglabs.jpaxos.dsl;

public class ClosedGroupException extends RuntimeException {

    private static final long serialVersionUID = -1558974081484525867L;

    public ClosedGroupException(String message) {
        super(message);
    }
}
