package io.messaginglabs.reaver.config;

import java.util.List;

public interface ConfigControl {

    ConfigView view();

    void join(List<Node> nodes);
    void leave();

    void add(ConfigEventsListener listener);
    void remove(ConfigEventsListener listener);

}
