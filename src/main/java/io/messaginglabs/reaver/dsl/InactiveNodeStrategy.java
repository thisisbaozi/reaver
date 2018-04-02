package io.messaginglabs.reaver.dsl;

public enum InactiveNodeStrategy {

    FIXED_DURATION,

    /*
     * Connects with this current in a increasing duration, members in config
     * will keep doing this unless applies a new config without this current
     *
     */
    INCREASING_DURATION,

    /*
     * Once a current is inactive and members in the config have reached on a
     * consensus about this, this current will be removed from current config,
     * a new config without this current will be applied.
     */
    REMOVE

}
