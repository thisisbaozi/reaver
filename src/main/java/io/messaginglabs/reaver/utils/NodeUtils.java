package io.messaginglabs.reaver.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class NodeUtils {

    private NodeUtils() {
        // singleton
    }

    public static long unsignedId(String ip, int port) {
        InetAddress netAddress;
        try {
            netAddress = InetAddress.getByName(ip);
            byte[] address = netAddress.getAddress();

            long id = 0;
            for (int i = 0; i < 4; i++) {
                id |= ((long)address[i] & 0xff) << (i * 8);
            }

            return (id << 31) | port;
        } catch (UnknownHostException e) {
            // ignore
        }

        return -1;
    }

}
