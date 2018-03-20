package io.messaginglabs.reaver.utils;

public final class AddressUtils {

    public static long composite(String ip, int port) {
        Parameters.checkNotEmpty(ip, "ip");

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

}
