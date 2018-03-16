package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.ServerConnector;
import io.messaginglabs.reaver.com.Transporter;
import io.messaginglabs.reaver.log.LogStorage;
import io.netty.buffer.ByteBufAllocator;
import java.util.concurrent.ScheduledExecutorService;

public class GroupEnv {

    public ByteBufAllocator allocator;

    /*
     * Either each group owns a exclusive storage, or share with other groups.
     */
    public LogStorage storage;

    /*
     * this executor is response for processing all events of a specified group
     */
    public ScheduledExecutorService executor;
    public ScheduledExecutorService applier;

    public Transporter transporter;
    public ServerConnector connector;

    /*
     * run with debug mode or not.
     */
    public boolean debug;

}
