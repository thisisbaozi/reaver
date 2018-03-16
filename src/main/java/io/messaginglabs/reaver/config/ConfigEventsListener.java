package io.messaginglabs.reaver.config;

public interface ConfigEventsListener {

    void onChanged(ConfigView view, ConfigChangedEvent event);
    void onLeaderChanged(ConfigView view, LeaderChangedEvent event);

}
