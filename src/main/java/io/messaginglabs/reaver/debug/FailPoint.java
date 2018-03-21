package io.messaginglabs.reaver.debug;

import io.messaginglabs.reaver.utils.Parameters;

public class FailPoint {

    public final String id;

    public FailPoint(String id) {
        this.id = Parameters.requireNotEmpty(id,"id");
    }

    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
