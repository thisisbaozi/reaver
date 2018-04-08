package io.messaginglabs.reaver;

import io.messaginglabs.reaver.com.DefaultServerConnector;
import io.messaginglabs.reaver.com.NettyTransporter;
import io.messaginglabs.reaver.com.RemoteServer;
import io.messaginglabs.reaver.com.Server;
import io.messaginglabs.reaver.com.ServerConnector;
import io.messaginglabs.reaver.com.msg.Reconfigure;
import io.messaginglabs.reaver.config.Member;
import io.messaginglabs.reaver.core.Defines;
import io.messaginglabs.reaver.core.Opcode;
import io.messaginglabs.reaver.utils.AddressUtils;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestNetwork {

    private final int port = 9666;
    private String ip;
    private NettyTransporter transporter;
    private LinkedBlockingQueue<ByteBuf> entries;

    @Before
    public void init() throws Exception {
        InetAddress address = AddressUtils.resolveIpV4();
        ip = address.getHostAddress();

        transporter = new NettyTransporter(ip, port, 1, "default");
        entries = new LinkedBlockingQueue<>();
        transporter.setConsumer(buf -> {
            buf.retain();
            entries.add(buf);
        });
        transporter.init();
    }

    @After
    public void close() throws Exception {
        transporter.close();
        entries.clear();
    }

    @Test
    public void testConnect() throws Exception {
        ServerConnector connector = new DefaultServerConnector(1, true, transporter);
        Server server = connector.connect(ip, port);
        server.connect();
        Thread.sleep(100);
        Assert.assertTrue(server.isActive());

        Reconfigure reconfigure = newReconfigure();
        server.send(reconfigure);

        Thread.sleep(100);
        Assert.assertEquals(entries.size(), 1);

        ByteBuf data = entries.poll();
        Reconfigure reconfigure1 = Reconfigure.decode(data);
        Assert.assertEquals(reconfigure.getMembers().size(), reconfigure1.getMembers().size());
        data.release();
    }

    private Reconfigure newReconfigure() {
        Reconfigure reconfigure = new Reconfigure();
        reconfigure.setGroupId(1);
        reconfigure.setOp(Opcode.JOIN_GROUP);
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MockUtils.newMember(ip, 4000 + 1, members);
        }
        reconfigure.setMembers(members);
        return reconfigure;
    }

    @Test
    public void testReleaseServer() throws Exception {
        ServerConnector connector = new DefaultServerConnector(-1, true, transporter);
        Server server = connector.connect(ip, port);
        server.connect();
        Thread.sleep(100);
        Server server1 = connector.connect(ip, port);
        Assert.assertTrue(server == server1);
        Assert.assertEquals(server.refCnt(), 2);
        server.release();
        Assert.assertTrue(server.isActive());
        server.release();
        Thread.sleep(100);
        Assert.assertFalse(server.isActive());

        Assert.assertNull(connector.get(ip, port));
        connector.release();

        connector = new DefaultServerConnector(1, true, transporter);
        server = connector.connect(ip, port);
        server.connect();

        server1 = connector.connect(ip, port);
        server1.connect();

        Thread.sleep(100);
        Assert.assertFalse(server == server1);
        Assert.assertEquals(connector.get(ip, port).size(), 2);
        server.release();
        server1.release();
    }

    @Test
    public void testSendAndReconnect() throws Exception {
        ServerConnector connector = new DefaultServerConnector(-1, true, transporter);
        Server server = connector.connect(ip, port);
        Reconfigure reconfigure = newReconfigure();
        server.send(reconfigure);
        Thread.sleep(100);
        Assert.assertNotNull(entries.poll());
        Assert.assertTrue(entries.isEmpty());

        // close and reconnect
        server.close();
        Thread.sleep(100);
        Assert.assertFalse(server.isActive());
        server.send(reconfigure);
        Thread.sleep(100);
        Assert.assertNotNull(entries.poll());
    }

    @Test
    public void testConnectTimeoutAndDropMessages() throws Exception {
        String ip = "128.0.0.1";
        ServerConnector connector = new DefaultServerConnector(-1, true, transporter);
        RemoteServer server = (RemoteServer)connector.connect(ip, port);
        server.set(1000, 12);

        Assert.assertFalse(server.future().isDone());

        Thread.sleep(Defines.CONNECT_TIMEOUT + 500);
        Assert.assertNull(server.future());

        Reconfigure reconfigure = newReconfigure();
        server.send(reconfigure);
        Assert.assertFalse(server.isActive());
        Assert.assertEquals(server.pendingSize(), 1);

        reconfigure = newReconfigure();
        server.send(reconfigure);
        Assert.assertEquals(server.pendingSize(), 2);
        
        // drop(capacity)
        for (int i = 0; i < 32; i++) {
            server.send(reconfigure);
        }
        Assert.assertEquals(server.pendingSize(), 12);

        // drop(expire)
        Thread.sleep(1200);
        server.send(reconfigure);
        Assert.assertEquals(server.pendingSize(), 1);
    }

    @Test
    public void testConnectionRecover() throws Exception {
        ServerConnector connector = new DefaultServerConnector(-1, true, transporter);
        RemoteServer server = (RemoteServer)connector.connect(ip, port);
        Thread.sleep(100);
        server.close();
        AtomicBoolean mark = new AtomicBoolean(false);
        server.set(integer -> {
            if (mark.get()) {
                return true;
            }
            if (integer >= 20) {
                mark.set(true);
                return true;
            }
            return false;
        });
        Reconfigure reconfigure = newReconfigure();

        for (int i = 0; i < 10; i++) {
            server.send(reconfigure);
        }
        Assert.assertEquals(server.pendingSize(), 10);
        for (int i = 0; i < 20; i++) {
            server.send(reconfigure);
        }
        Thread.sleep(400);
        Assert.assertEquals(server.pendingSize(), 0);
    }


}
