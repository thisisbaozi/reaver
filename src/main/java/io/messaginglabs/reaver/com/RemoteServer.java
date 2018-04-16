package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.messaginglabs.reaver.utils.NodeUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteServer extends AbstractReferenceCounted implements Server {

    private static final Logger logger = LoggerFactory.getLogger(RemoteServer.class);

    private final String ip;
    private final int port;
    private final long nodeId;
    private final boolean debug;

    private int deadline = 1500;
    private int maxCapacity = 2048;
    private int times = 0;
    private long beginConnect = -1;
    private Function<Integer, Boolean> beforeConnect;

    private Channel ch;
    private ChannelFuture future;
    private Transporter transporter;

    final class Entry {
        final long begin;
        final ByteBuf data;

        Entry(ByteBuf data) {
            this.begin = System.currentTimeMillis();
            this.data = data;
        }
    }

    private List<Entry> messages;

    public RemoteServer(String ip, int port, boolean debug, Transporter bootstrap) {
        this.ip = ip;
        this.port = port;
        this.debug = debug;
        this.transporter = bootstrap;
        this.nodeId = NodeUtils.unsignedId(ip, port);

        connect();
    }

    public void set(int deadline, int maxCapacity) {
        this.deadline = deadline;
        this.maxCapacity = maxCapacity;
    }

    public void set(Function<Integer, Boolean> beforeConnect) {
        this.beforeConnect = beforeConnect;
    }

    @Override
    protected void deallocate() {
        try {
            close();
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String address() {
        return String.format("%s:%d", ip, port);
    }

    @Override
    public long nodeId() {
        return nodeId;
    }

    public int pendingSize() {
        synchronized (this) {
            return messages == null ? 0 : messages.size();
        }
    }

    @Override
    public void close() throws Exception {
        if (future != null && !future.isDone()) {
            /*
             * cancel if it's connecting with remote peer
             */
            if (future.isCancellable()) {
                future.cancel(true);
            }
        }

        if (ch != null && ch.isOpen()) {
            ch.close();
        }
    }

    private ByteBuf serialize(Message msg) {
        Objects.requireNonNull(msg, "msg");

        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(1024);

        int size = msg.size();
        buf.writeInt(size);

        int idx = buf.writerIndex();
        int currentIdx = msg.encode(buf).writerIndex();
        if (currentIdx - idx != size) {
            throw new IllegalStateException(
                String.format("msg size(%d), serialized size(%d), msg(%s)", size, currentIdx - idx, msg.toString())
            );
        }

        return buf;
    }

    public ChannelFuture future() {
        return future;
    }

    @Override
    public boolean isActive() {
        return ch != null && ch.isActive();
    }

    @Override
    public boolean connect(long timeout) throws InterruptedException {
        synchronized (this) {
            if (isActive()) {
                return true;
            }

            connect();

            // blocking until the connection is completed
            ChannelFuture future = this.future;
            if (future != null) {
                if (!future.await(timeout, TimeUnit.MILLISECONDS)) {
                    return false;
                }

                if (future.isSuccess()) {
                    return future.channel() != null && future.channel().isActive();
                }
            }

            return isActive();
        }
    }

    @Override
    public void connect() {
        synchronized (this) {
            if (isActive()) {
                return ;
            }

            // it's connecting?
            if (future != null) {
                if (future.isDone()) {
                    ch = future.channel();
                }

                if (System.currentTimeMillis() - beginConnect < 2000) {
                    return ;
                }

                if (future.isCancellable()) {
                    future.cancel(true);
                }
            }

            ch = null;
            times++;

            // testing
            if (beforeConnect != null && !beforeConnect.apply(times)) {
                return ;
            }

            beginConnect = System.currentTimeMillis();
            future = transporter.connect(ip, port).addListener((ChannelFutureListener)future -> {
                this.future = null;

                if (future.isSuccess()) {
                    this.times = 0;
                    this.ch = future.channel();
                    if (future.isSuccess()) {
                        int count = emitPending();
                        if (count > 0 && logger.isDebugEnabled()) {
                            logger.info("emit {} pending message after connection({}) is connected", count, ch.toString());
                        }
                    }

                    return ;
                }

                if (logger.isWarnEnabled()) {
                    logger.warn("can't connect with address({}:{})", ip, port, future.cause());
                }
            });
        }
    }

    private void cache(ByteBuf data) {
        synchronized (this) {
            if (messages == null) {
                messages = new ArrayList<>(1024);
            }

            messages.add(new Entry(data));
            if (messages.size() == 1) {
                return ;
            }

            long current = System.currentTimeMillis();
            while (messages.size() > 1) {
                Entry entry = messages.get(0);
                if (current - entry.begin >= deadline) {
                    remove(messages);
                    continue;
                }

                if (messages.size() > maxCapacity) {
                    remove(messages);
                } else {
                    break;
                }
            }
        }
    }

    private static void remove(List<Entry> messages) {
        Entry entry = messages.remove(0);
        if (entry != null) {
            if (entry.data.refCnt() != 1) {
                throw new IllegalStateException(
                    String.format("ref count is not 1 but %d", entry.data.refCnt())
                );
            }

            entry.data.release();
        }
    }

    private int emitPending() {
        if (ch == null) {
            throw new IllegalStateException("buggy, channel of server is still null");
        }

        synchronized (this) {
            if (messages == null || messages.isEmpty()) {
                return 0;
            }

            for (Entry entry : messages) {
                doWrite(ch, entry.data);
            }

            int size = messages.size();
            messages.clear();

            return size;
        }
    }

    @Override
    public void send(Message msg) {
        ByteBuf buf = serialize(msg);

        if (!isActive()) {
            cache(buf);
            connect();
            return ;
        }

        doWrite(ch, buf);
    }

    @Override
    public void send(Message msg, long timeout) throws TimeoutException, InterruptedException {
        ByteBuf buf = serialize(msg);

        if (!isActive()) {
            cache(buf);
            connect();
            return ;
        }

        ch.writeAndFlush(buf).await(timeout, TimeUnit.MILLISECONDS);
    }

    private void doWrite(Channel ch, ByteBuf buf) {
        if (debug) {
            ch.writeAndFlush(buf).addListener((ChannelFutureListener)future -> {
                boolean result = future.isSuccess();
                if (result && logger.isWarnEnabled()) {
                    logger.warn("can't send message to server({}:{})", ip, port, future.cause());
                }
            });
        } else {
            ch.writeAndFlush(buf, ch.voidPromise());
        }
    }

    @Override
    public void areYouOk() {

    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }
}
