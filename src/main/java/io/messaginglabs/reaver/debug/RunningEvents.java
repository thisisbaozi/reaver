package io.messaginglabs.reaver.debug;

public interface RunningEvents {

    void add(RunningEvent event);

    void dump(int begin, int end);
    void dump();

}
