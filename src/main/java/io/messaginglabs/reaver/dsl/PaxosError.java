package io.messaginglabs.reaver.dsl;

public class PaxosError extends Error {

    private static final long serialVersionUID = -2610864975587993869L;

    public PaxosError(String message) {
        super(message);
    }
}
