package io.messaginglabs.reaver.dsl;

public class FrozenGroupException extends RuntimeException {

    private static final long serialVersionUID = 8200466234010935643L;

    public FrozenGroupException(String message) {
        super(message);
    }
}
