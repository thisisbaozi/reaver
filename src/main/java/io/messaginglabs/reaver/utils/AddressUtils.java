package io.messaginglabs.reaver.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

public final class AddressUtils {

    private static InetAddress localAddress = null;

    public static long composite(String ip, int port) {
        Parameters.requireNotEmpty(ip, "ip");

        long value = 0;
        String[] segments = ip.split("\\.");
        for (String seg : segments) {
            int number = Integer.parseInt(seg);
            value = value << 8;
            value |= number;
        }

        value = value << 32;
        value |= port;

        return value;
    }

    public static String toString(long address) {
        return String.format("%d.%d.%d.%d", (address >> 24 + 32) & 0xFF, (address >> 16 + 32) & 0xFF, (address >> 8 + 32) & 0xFF, (address >> 32) & 0xFF);
    }

    public static InetAddress resolveIpV4() throws SocketException {
        if (localAddress != null) {
            return localAddress;
        }

        ArrayList<InetAddress> interfaces = new ArrayList<>();

        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface netInterface = enumeration.nextElement();
            Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address.isLoopbackAddress() || address instanceof Inet6Address) {
                    continue;
                }

                interfaces.add(address);
            }
        }

        if (interfaces.isEmpty()) {
            return null;
        }

        for (InetAddress address : interfaces) {
            if (address.getHostAddress() == null) {
                continue;
            }

            localAddress = address;
            return address;
        }

        localAddress = interfaces.get(interfaces.size() - 1);
        return localAddress;
    }

}
