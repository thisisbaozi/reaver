package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.com.ServerConnector;
import io.messaginglabs.reaver.com.Transporter;
import io.messaginglabs.reaver.core.AlgorithmPhase;
import io.messaginglabs.reaver.core.V0ValueCodec;
import io.messaginglabs.reaver.core.ValueCodec;
import io.messaginglabs.reaver.log.LogStorage;
import io.netty.buffer.ByteBufAllocator;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;

public class GroupEnv {

    private static ValueCodec DEFAULT_CODEC = new V0ValueCodec();

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
    public AlgorithmPhase phase = AlgorithmPhase.TWO_PHASE;
    public ValueCodec codec = DEFAULT_CODEC;

    /*
     * run with debug mode or not.
     */
    public boolean debug = true;

    public Logger logger;

}
