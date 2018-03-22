package io.messaginglabs.reaver.dsl;

public enum InactiveNodeStrategy {

    FIXED_DURATION,

    /*
     * Connects with this node in a increasing duration, members in config
     * will keep doing this unless applies a new config without this node
     *
     */
    INCREASING_DURATION,

    /*
     * Once a node is inactive and members in the config have reached on a
     * consensus about this, this node will be removed from current config,
     * a new config without this node will be applied.
     */
    REMOVE

}
