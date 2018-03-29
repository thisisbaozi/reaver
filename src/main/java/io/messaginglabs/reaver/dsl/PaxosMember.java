package io.messaginglabs.reaver.dsl;

public interface PaxosMember {

    /**
     * Returns the address of this member in format(ip:port), the returned
     * address never be null
     */
    String address();

    /**
     * The minimal/maximum instance id this group maintained or processed, Both
     * id are not accurate absolutely.
     */
    long minInstanceId();
    long maxInstanceId();

}
