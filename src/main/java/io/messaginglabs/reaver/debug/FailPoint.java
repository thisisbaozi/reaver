package io.messaginglabs.reaver.debug;

public class FailPoint {

    private final String id;
    private final int probability;

    public FailPoint(String id, int probability) {
        this.id = id;
        this.probability = probability;
    }

    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

}
