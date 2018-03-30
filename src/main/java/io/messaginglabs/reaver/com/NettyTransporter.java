package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.core.Defines;
import io.messaginglabs.reaver.utils.Parameters;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyTransporter extends AbstractReferenceCounted implements Transporter {

    private static final Logger logger = LoggerFactory.getLogger(NettyTransporter.class);

    private final String prefix;
    private final String address;
    private final int port;
    private final int threads;

    private NioEventLoopGroup executors;
    private Consumer<ByteBuf> framesConsumer;
    private Channel ch;

    private Bootstrap bootstrap;

    public NettyTransporter(String address, int port, int threads, String prefix) {
        Parameters.requireNotEmpty(address, "address");

        if (port <= 0) {
            throw new IllegalArgumentException("invalid port");
        }

        if (threads <= 0) {
            throw new IllegalArgumentException("the number of io threads must be greater than 0, but given: " + threads);
        }

        this.address = address;
        this.port = port;
        this.threads = threads;
        this.prefix = prefix;
    }

    @Override
    public void init() throws Exception {
        executors = new NioEventLoopGroup(threads, new DefaultThreadFactory(prefix + "-transport"));

        try {
            initServerBootstrap(executors);
        } catch (Exception cause) {
            executors.shutdownGracefully();
            throw cause;
        }

        initBootstrap(executors);
    }

    private void initServerBootstrap(NioEventLoopGroup executors) throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

        // bind
        bootstrap.localAddress(address, port);
        bootstrap.group(executors, executors).channel(NioServerSocketChannel.class);

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                    new LengthFieldBasedFrameDecoder(
                        Defines.PACKET_MAX_SIZE, 0, 4, 0, 4
                    )
                );
                ch.pipeline().addLast(new Dispatcher());
            }
        });

        ch = bootstrap.bind().sync().channel();
    }

    private void initBootstrap(NioEventLoopGroup executors) {
        bootstrap = new Bootstrap();
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Defines.CONNECT_TIMEOUT);
        bootstrap.group(executors).channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                    new LengthFieldBasedFrameDecoder(
                        Defines.PACKET_MAX_SIZE, 0, 4, 0, 4
                    )
                );
                ch.pipeline().addLast(new Dispatcher());
            }
        });
    }

    final class Dispatcher extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            try {
                framesConsumer.accept(msg);
            } catch (Exception cause) {
                if (logger.isWarnEnabled()) {
                    logger.warn("can't process frame, caught unknown exception", cause);
                }
            }
        }
    }

    @Override
    public ChannelFuture connect(String ip, int port) {
        return bootstrap.connect(ip, port);
    }

    @Override
    public void close() throws Exception {
        // stop accepting new channels
        ch.close().sync();

        // process pending frames in channels and stop reading data from channels
        executors.shutdownGracefully();

        // break active channels?
    }

    @Override
    public void setConsumer(Consumer<ByteBuf> msgConsumer) {
        this.framesConsumer = msgConsumer;
    }

    @Override
    protected void deallocate() {
        try {
            close();
        } catch (Exception cause) {
            // ignore
        }
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

}
