package io.messaginglabs.reaver.dsl;

public enum ElectionPolicy {

    /**
     * Selects a leader from active members randomly
     */
    RANDOM,

    /**
     * Sorts active members based on address and selects the first one as
     * the new leader.
     */
    FIRST_IP,

    /**
     * Selects the member who's instance it processed is maximum than other members
     */
    MAX_INSTANCES_FIRST,

    /**
     * User must specify a implementation of {@link LeaderSelector} while
     * building a new group.
     */
    CUSTOMIZED,

}
