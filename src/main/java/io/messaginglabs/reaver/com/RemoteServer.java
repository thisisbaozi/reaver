package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.com.msg.Message;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteServer extends AbstractReferenceCounted implements Server {

    private static final Logger logger = LoggerFactory.getLogger(RemoteServer.class);

    private final String ip;
    private final int port;
    private final boolean debug;

    private int deadline = 1500;
    private long beginConnect = -1;

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

    public RemoteServer(String ip, int port, boolean debug, Transporter transporter) {
        this.ip = ip;
        this.port = port;
        this.debug = debug;
        this.transporter = Objects.requireNonNull(transporter, "transporter");
        this.future = this.transporter.connect(ip, port);
    }

    private void setDeadline(int deadline) {
        this.deadline = deadline;
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
        int currentIdx = msg.encode(buf).readerIndex();
        if (currentIdx - idx != size) {
            throw new IllegalStateException(
                String.format("msg size(%d), serialized size(%d), msg(%s)", size, currentIdx - idx, msg.toString())
            );
        }

        return buf;
    }

    private boolean isWritable() {
        return ch != null && ch.isActive();
    }

    private void connect() {
        synchronized (this) {
            if (isWritable()) {
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
            beginConnect = System.currentTimeMillis();
            future = transporter.connect(ip, port).addListener((ChannelFutureListener)future -> {
                this.ch = future.channel();
                this.future = null;
                if (future.isSuccess()) {
                    emitPending();
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

            int size = messages.size();
            if (size == 1) {
                return ;
            }

            long current = System.currentTimeMillis();
            for (;;) {
                Entry entry = messages.get(0);
                if (current - entry.begin >= deadline) {
                    messages.remove(0);
                } else {
                    break;
                }
            }
        }
    }

    private void emitPending() {
        if (ch == null) {
            throw new IllegalStateException("buggy, channel of server is still null");
        }

        synchronized (this) {
            if (messages.isEmpty()) {
                return ;
            }

            for (Entry entry : messages) {
                doWrite(ch, entry.data);
            }
        }
    }

    @Override
    public void send(Message msg) {
        ByteBuf buf = serialize(msg);

        if (!isWritable()) {
            cache(buf);
            connect();
            return ;
        }

        doWrite(ch, buf);
    }

    private void doWrite(Channel ch, ByteBuf buf) {
        if (debug) {
            ch.write(buf).addListener((ChannelFutureListener)future -> {
                boolean result = future.isSuccess();
                if (result && logger.isWarnEnabled()) {
                    logger.warn("can't send message to server({}:{})", ip, port, future.cause());
                }
            });
        } else {
            ch.write(buf, ch.voidPromise());
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
